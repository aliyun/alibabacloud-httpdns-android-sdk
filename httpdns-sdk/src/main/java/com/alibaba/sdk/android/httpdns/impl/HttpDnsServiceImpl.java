package com.alibaba.sdk.android.httpdns.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.sdk.android.crashdefend.CrashDefendApi;
import com.alibaba.sdk.android.crashdefend.CrashDefendCallback;
import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsCallback;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.resolve.HostFilter;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostCacheGroup;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostRequestHandler;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResultRepo;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostService;
import com.alibaba.sdk.android.httpdns.resolve.BatchResolveHostService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService.OnRegionServerIpUpdate;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.content.Context;
import android.os.Looper;

/**
 * 域名解析服务 httpdns接口的实现
 */
public class HttpDnsServiceImpl implements HttpDnsService, OnRegionServerIpUpdate,
	NetworkStateManager.OnNetworkChange, SyncService {

	protected HttpDnsConfig mHttpDnsConfig;
	private ResolveHostResultRepo mResultRepo;
	protected ResolveHostRequestHandler mRequestHandler;
	protected RegionServerScheduleService mScheduleService;
	protected IPRankingService mIpIPRankingService;
	protected ResolveHostService mResolveHostService;
	protected BatchResolveHostService mBatchResolveHostService;
	private HostFilter mFilter;
	private SignService mSignService;
	private boolean resolveAfterNetworkChange = true;
    /**
	 * crash defend 默认关闭
	 */
	private boolean mCrashDefendEnabled = false;

	public HttpDnsServiceImpl(Context context, final String accountId, String secret) {
		try {
			mHttpDnsConfig = new HttpDnsConfig(context, accountId);
			mFilter = new HostFilter();
			mSignService = new SignService(secret);
			mIpIPRankingService = new IPRankingService(this.mHttpDnsConfig);
			mResultRepo = new ResolveHostResultRepo(this.mHttpDnsConfig, this.mIpIPRankingService,
				new RecordDBHelper(this.mHttpDnsConfig.getContext(), this.mHttpDnsConfig.getAccountId()),
				new ResolveHostCacheGroup());
			mScheduleService = new RegionServerScheduleService(this.mHttpDnsConfig, this);
			mRequestHandler = new ResolveHostRequestHandler(mHttpDnsConfig, mScheduleService,
                mSignService);
			HostResolveLocker asyncLocker = new HostResolveLocker();
			mResolveHostService = new ResolveHostService(this.mHttpDnsConfig,
				mIpIPRankingService,
                mRequestHandler, mResultRepo, mFilter, asyncLocker);
			mBatchResolveHostService = new BatchResolveHostService(this.mHttpDnsConfig, mResultRepo,
                mRequestHandler,
				mIpIPRankingService, mFilter, asyncLocker);
			mHttpDnsConfig.setNetworkDetector(HttpDnsNetworkDetector.getInstance());

			beforeInit();

			setupInitConfig(accountId);

			if (mCrashDefendEnabled) {
				initCrashDefend(context, mHttpDnsConfig);
			}
			if (!mHttpDnsConfig.isEnabled()) {
				HttpDnsLog.w("init fail, crash defend");
				return;
			}
			NetworkStateManager.getInstance().init(context);
			NetworkStateManager.getInstance().addListener(this);
			if (mHttpDnsConfig.getCurrentServer().shouldUpdateServerIp()
				|| !mHttpDnsConfig.isCurrentRegionMatch()) {
				mScheduleService.updateRegionServerIps();
			}
			favorInit(context, accountId);

			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("httpdns service is init " + accountId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupInitConfig(final String accountId) {
		InitConfig config = InitConfig.getInitConfig(accountId);

		if (config != null) {
			// 先设置和网络相关的内容
			this.mHttpDnsConfig.setTimeout(config.getTimeout());
			this.mHttpDnsConfig.setHTTPSRequestEnabled(config.isEnableHttps());
			// 再设置一些可以提前，没有副作用的内容
			mResolveHostService.setEnableExpiredIp(config.isEnableExpiredIp());
			if (config.getIPRankingList() != null) {
				mIpIPRankingService.setIPRankingList(config.getIPRankingList());
			}
			// 设置region 必须在 读取缓存之前
			if (config.getRegion() != InitConfig.NOT_SET) {
				this.mHttpDnsConfig.setRegion(config.getRegion());
			}
			// 设置 主站域名 需要在 读取缓存之前
			this.mResultRepo.setHostListWhichIpFixed(config.getHostListWithFixedIp());
			// 设置缓存控制，并读取缓存
			mResultRepo.setCachedIPEnabled(config.isEnableCacheIp(), Constants.DEFAULT_ENABLE_AUTO_CLEAN_CACHE_AFTER_LOAD);
			this.mResultRepo.setCacheTtlChanger(config.getCacheTtlChanger());
			resolveAfterNetworkChange = config.isResolveAfterNetworkChange();

			if (config.getDegradationFilter() != null) {
				mFilter.setFilter(config.getDegradationFilter());
			}

			if (config.getNotUseHttpDnsFilter() != null) {
				mFilter.setFilter(config.getNotUseHttpDnsFilter());
			}

			mCrashDefendEnabled = config.isEnableCrashDefend();

			mRequestHandler.setSdnsGlobalParams(config.getSdnsGlobalParams());
		}

	}

	protected void beforeInit() {
		// only for test
	}

	protected void favorInit(Context context, String accountId) {
		// for different favor init
	}

	protected void initCrashDefend(Context context, final HttpDnsConfig config) {
		CrashDefendApi.registerCrashDefendSdk(context, "httpdns", BuildConfig.VERSION_NAME, 2, 7,
			new CrashDefendCallback() {
				@Override
				public void onSdkStart(int limitCount, int crashCount, int restoreCount) {
					config.crashDefend(false);
				}

				@Override
				public void onSdkStop(int limitCount, int crashCount, int restoreCount,
									  long nextRestoreInterval) {
					config.crashDefend(true);
					HttpDnsLog.w("sdk is not safe to run");
				}

				@Override
				public void onSdkClosed(int restoreCount) {
					config.crashDefend(true);
					HttpDnsLog.e("sdk will not run any more");
				}
			});
	}

	public void setSecret(String secret) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		if (secret == null || secret.equals("")) {
			HttpDnsLog.e("set empty secret!?");
		}
		this.mSignService.setSecret(secret);
	}

	@Override
	public void serverIpUpdated(boolean regionUpdated) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		if (regionUpdated) {
			mResultRepo.clearMemoryCache();
		}
		mRequestHandler.resetStatus();
	}

	@Override
	public void setPreResolveHosts(List<String> hostList) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return;
		}
		if (hostList == null || hostList.size() == 0) {
			HttpDnsLog.i("setPreResolveHosts empty list");
			return;
		}
		setPreResolveHosts(hostList, RequestIpType.v4);
	}

	@Override
	public void setPreResolveHosts(List<String> hostList, RequestIpType requestIpType) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return;
		}
		if (hostList == null || hostList.size() == 0) {
			HttpDnsLog.i("setPreResolveHosts empty list");
			return;
		}
		requestIpType = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), requestIpType);
		mBatchResolveHostService.batchResolveHostAsync(hostList, requestIpType);
	}

	@Override
	public String getIpByHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return null;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return null;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return null;
		}
		String[] ips = getIPv4ListForHostAsync(host);
		if (ips == null || ips.length == 0) {
			return null;
		}
		return ips[0];
	}

	@Override
	public String getIPv4ForHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return null;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return null;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return null;
		}
		String[] ips = getIPv4ListForHostAsync(host);
		if (ips == null || ips.length == 0) {
			return null;
		}
		return ips[0];
	}

	@Override
	public String[] getIpsByHostAsync(final String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return new String[0];
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return new String[0];
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return new String[0];
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, null, null).getIps();
	}

	@Override
	public String[] getIPv4ListForHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return new String[0];
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return new String[0];
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return new String[0];
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, null, null).getIps();
	}

	@Override
	public String[] getIPv6sByHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return new String[0];
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return new String[0];
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return new String[0];
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v6, null, null)
			.getIpv6s();
	}

	@Override
	public String[] getIPv6ListForHostASync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return new String[0];
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return new String[0];
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return new String[0];
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v6, null, null)
			.getIpv6s();
	}

	@Override
	public HTTPDNSResult getAllByHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.both, null, null);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.both, null, null);
	}

	@Override
	public HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
	}

	@Override
	public void setAuthCurrentTime(long time) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mSignService.setCurrentTimestamp(time);
	}

	@Override
	public String getSessionId() {
		return SessionTrackMgr.getInstance().getSessionId();
	}

	@Override
	public HTTPDNSResult getIpsByHostAsync(String host, Map<String, String> params,
										   String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, params, cacheKey);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostAsync(String host, Map<String, String> params,
													  String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, params, cacheKey);
	}

	@Override
	public HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type,
										   Map<String, String> params, String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, params, cacheKey);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type,
													  Map<String, String> params, String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, params, cacheKey);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type, Map<String, String> params, String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		if (Looper.getMainLooper() == Looper.myLooper()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request in main thread, use async request");
			}
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
		}
		return mResolveHostService.resolveHostSync(host, type, params, cacheKey);
	}

	@Override
	public void getHttpDnsResultForHostAsync(String host, RequestIpType type, Map<String, String> params, String cacheKey, HttpDnsCallback callback) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}

		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		mResolveHostService.resolveHostAsync(host, type, params, cacheKey, callback);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type, Map<String, String> params, String cacheKey) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, params, cacheKey);
	}

	@Override
	public void setRegion(String region) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return;
		}
		region = CommonUtil.fixRegion(region);
		if (CommonUtil.regionEquals(this.mHttpDnsConfig.getRegion(), region)
			&& this.mHttpDnsConfig.isCurrentRegionMatch()
			&& !this.mHttpDnsConfig.isAllInitServer()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("region " + region + " is same, do not update serverIps");
			}
			return;
		}
		boolean changed = mHttpDnsConfig.setRegion(region);
		if (changed) {
			mResultRepo.clearMemoryCache();
		}
		mScheduleService.updateRegionServerIps(region);
	}

	@Override
	public String getIPv6ByHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return null;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return null;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return null;
		}
		String[] ips = getIPv6sByHostAsync(host);
		if (ips == null || ips.length == 0) {
			return null;
		}
		return ips[0];
	}

	@Override
	public String getIPv6ForHostAsync(String host) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return null;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return null;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return null;
		}
		String[] ips = getIPv6sByHostAsync(host);
		if (ips == null || ips.length == 0) {
			return null;
		}
		return ips[0];
	}

	@Override
	public void onNetworkChange(String networkType) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		try {
			mHttpDnsConfig.getWorker().execute(new Runnable() {
				@Override
				public void run() {
					HashMap<String, RequestIpType> allHost = mResultRepo.getAllHostWithoutFixedIP();
					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.d("network change, clean record");
					}
					mResultRepo.clearMemoryCacheForHostWithoutFixedIP();
					if (resolveAfterNetworkChange && mHttpDnsConfig.isEnabled()) {
						ArrayList<String> v4List = new ArrayList<>();
						ArrayList<String> v6List = new ArrayList<>();
						ArrayList<String> bothList = new ArrayList<>();
						for (Map.Entry<String, RequestIpType> entry : allHost.entrySet()) {
							if (entry.getValue() == RequestIpType.v4) {
								v4List.add(entry.getKey());
							} else if (entry.getValue() == RequestIpType.v6) {
								v6List.add(entry.getKey());
							} else {
								bothList.add(entry.getKey());
							}
						}
						if (v4List.size() > 0) {
							mBatchResolveHostService.batchResolveHostAsync(v4List, RequestIpType.v4);
						}
						if (v6List.size() > 0) {
							mBatchResolveHostService.batchResolveHostAsync(v6List, RequestIpType.v6);
						}
						if (bothList.size() > 0) {
							mBatchResolveHostService.batchResolveHostAsync(bothList, RequestIpType.both);
						}
						if (v4List.size() > 0 || v6List.size() > 0 || bothList.size() > 0) {
							if (HttpDnsLog.isPrint()) {
								HttpDnsLog.d("network change, resolve hosts");
							}
						}
					}
				}
			});
		} catch (Exception e) {
		}
	}

	@Override
	public HTTPDNSResult getByHost(String host, RequestIpType type) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		if (Looper.getMainLooper() == Looper.myLooper()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request in main thread, use async request");
			}
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
		}
		return mResolveHostService.resolveHostSync(host, type, null, null);
	}

	private RequestIpType changeTypeWithNetType(HttpDnsSettings.NetworkDetector networkDetector,
												RequestIpType type) {
		if (type == RequestIpType.auto) {
			if (networkDetector != null) {
				switch (networkDetector.getNetType(mHttpDnsConfig.getContext())) {
					case v6:
						return RequestIpType.v6;
					case v4:
						return RequestIpType.v4;
					default:
						return RequestIpType.both;
				}
			}
			return RequestIpType.both;
		}
		return type;
	}

	public void cleanHostCache(ArrayList<String> hosts) {
		if (hosts == null || hosts.size() == 0) {
			// 清理所有host
			mResultRepo.clear();
		} else {
			// 清理选中的host
			mResultRepo.clear(hosts);
		}
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		if (Looper.getMainLooper() == Looper.myLooper()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("request in main thread, use async request");
			}
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
		}
		return mResolveHostService.resolveHostSync(host, type, null, null);
	}

	@Override
	public void getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			if (callback != null) {
				callback.onHttpDnsCompleted(Constants.EMPTY);
			}
			return;
		}

		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);

		mResolveHostService.resolveHostAsync(host, type, null, null, callback);
	}

	@Override
	public HTTPDNSResult getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return Constants.EMPTY;
		}
		if (!CommonUtil.isAHost(host)) {
			HttpDnsLog.i("host is invalid. " + host);
			return Constants.EMPTY;
		}
		if (CommonUtil.isAnIP(host)) {
			HttpDnsLog.i("host is ip. " + host);
			return Constants.EMPTY;
		}
		type = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), type);
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, null);
	}
}
