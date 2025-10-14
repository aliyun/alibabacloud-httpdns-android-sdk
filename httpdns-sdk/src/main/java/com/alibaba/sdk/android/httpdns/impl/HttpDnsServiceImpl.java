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
import com.alibaba.sdk.android.httpdns.Region;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;
import com.alibaba.sdk.android.httpdns.observable.event.CleanHostCacheEvent;
import com.alibaba.sdk.android.httpdns.resolve.HostFilter;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostCache;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostCacheGroup;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostRequestHandler;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResultRepo;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostService;
import com.alibaba.sdk.android.httpdns.resolve.BatchResolveHostService;
import com.alibaba.sdk.android.httpdns.HTTPDNSResultWrapper;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService.OnRegionServerIpUpdate;
import com.alibaba.sdk.android.httpdns.serverip.ranking.RegionServerRankingService;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.content.Context;
import android.content.SharedPreferences;
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
	protected RegionServerRankingService mRegionServerRankingService;
	protected ResolveHostService mResolveHostService;
	protected BatchResolveHostService mBatchResolveHostService;
	private HostFilter mFilter;
	private SignService mSignService;
	private AESEncryptService mAESEncryptService;
	private boolean resolveAfterNetworkChange = true;
    /**
	 * crash defend 默认关闭
	 */
	private boolean mCrashDefendEnabled = false;
	public static Context sContext;

	public HttpDnsServiceImpl(Context context, final String accountId, String secret) {
		try {
			InitConfig config = InitConfig.getInitConfig(accountId);
			sContext = (config != null && config.getContext() != null) ? config.getContext() : context;
			secret = (config != null && config.getSecretKey() != null) ? config.getSecretKey() : secret;
			mHttpDnsConfig = new HttpDnsConfig(sContext, accountId, secret);
			if (sContext == null) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("init httpdns with null context!!");
				}
				mHttpDnsConfig.setEnabled(false);
				return;
			}
			mFilter = new HostFilter();
			mSignService = new SignService(secret);
			mAESEncryptService = new AESEncryptService();
			mIpIPRankingService = new IPRankingService(this.mHttpDnsConfig);
			mRegionServerRankingService = new RegionServerRankingService(mHttpDnsConfig);
			mResultRepo = new ResolveHostResultRepo(this.mHttpDnsConfig, this.mIpIPRankingService,
				new RecordDBHelper(this.mHttpDnsConfig.getContext(), this.mHttpDnsConfig.getAccountId()),
				new ResolveHostCacheGroup());
			mScheduleService = new RegionServerScheduleService(this.mHttpDnsConfig, this);
			mRequestHandler = new ResolveHostRequestHandler(mHttpDnsConfig, mScheduleService,
                mSignService, mAESEncryptService);
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
				initCrashDefend(sContext, mHttpDnsConfig);
			}
			if (!mHttpDnsConfig.isEnabled()) {
				HttpDnsLog.w("init fail, crash defend");
				return;
			}
			NetworkStateManager.getInstance().init(sContext);
			NetworkStateManager.getInstance().addListener(this);

			tryUpdateRegionServer(sContext, accountId);

			mRegionServerRankingService.rankServiceIp(mHttpDnsConfig.getCurrentServer());
			favorInit(sContext, accountId);

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
			mHttpDnsConfig.setBizTags(config.getBizTags());
			mHttpDnsConfig.setEnableDegradationLocalDns(config.isEnableDegradationLocalDns());
			// 再设置一些可以提前，没有副作用的内容
			mResolveHostService.setEnableExpiredIp(config.isEnableExpiredIp());
			if (config.getIPRankingList() != null) {
				mIpIPRankingService.setIPRankingList(config.getIPRankingList());
			}
			// 设置region 必须在 读取缓存之前。2.4.1版本开始region初始化提前到HttpDnsConfig初始化

			// 设置 主站域名 需要在 读取缓存之前
			this.mResultRepo.setHostListWhichIpFixed(config.getHostListWithFixedIp());
			// 设置缓存控制，并读取缓存
			mResultRepo.setCachedIPEnabled(config.isEnableCacheIp(), config.getExpiredThresholdMillis());
			this.mResultRepo.setCacheTtlChanger(config.getCacheTtlChanger());
			resolveAfterNetworkChange = config.isResolveAfterNetworkChange();

			if (config.getDegradationFilter() != null) {
				mFilter.setFilter(config.getDegradationFilter());
			}

			if (config.getNotUseHttpDnsFilter() != null) {
				mFilter.setFilter(config.getNotUseHttpDnsFilter());
			}

			mHttpDnsConfig.getObservableManager().positiveEnableObservable(config.isEnableObservable());

			mCrashDefendEnabled = config.isEnableCrashDefend();

			mRequestHandler.setSdnsGlobalParams(config.getSdnsGlobalParams());
			mAESEncryptService.setAesSecretKey(config.getAesSecretKey());
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
		InitConfig config = InitConfig.getInitConfig(mHttpDnsConfig.getAccountId());
		secret = (config != null && config.getSecretKey() != null) ? config.getSecretKey() : secret;
		this.mSignService.setSecretKey(secret);
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

		//服务IP更新，触发服务IP测速
		mRegionServerRankingService.rankServiceIp(mHttpDnsConfig.getCurrentServer());
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, null, getCurrentNetworkKey()).getIps();
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v4, null, getCurrentNetworkKey()).getIps();
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v6, null, getCurrentNetworkKey())
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.v6, null, getCurrentNetworkKey())
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.both, null, getCurrentNetworkKey());
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, RequestIpType.both, null, getCurrentNetworkKey());
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
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
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
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
			&& !this.mHttpDnsConfig.isAllInitServer()) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("region " + region + " is same, do not update serverIps");
			}
			return;
		}
		boolean changed = mHttpDnsConfig.setRegion(region);
		if (changed) {
			mResultRepo.clearMemoryCache();
			//region变化，服务IP变成对应的预置IP，触发测速
			mRegionServerRankingService.rankServiceIp(mHttpDnsConfig.getCurrentServer());
		}
		mScheduleService.updateRegionServerIps(region, Constants.UPDATE_REGION_SERVER_SCENES_REGION_CHANGE);
	}

	@Override
	public void setRegion(Region region) {
		setRegion(region == null ? "" : region.getRegion());
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
                    // 获取当前网络标识
                    String requestNetworkKey = getCurrentNetworkKey();

                    // 获取历史域名
					HashMap<String, RequestIpType> allHost = mResultRepo.getAllHostWithoutFixedIP();

					if (HttpDnsLog.isPrint()) {
						HttpDnsLog.d("network change to " + requestNetworkKey + ", smart resolve hosts");
					}

                    // 智能增量解析
					if (resolveAfterNetworkChange && mHttpDnsConfig.isEnabled()) {
						smartBatchResolve(allHost, requestNetworkKey);
					}
				}
			});

			//网络变化，触发服务IP测速
			mRegionServerRankingService.rankServiceIp(mHttpDnsConfig.getCurrentServer());
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
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
		}
		return mResolveHostService.resolveHostSync(host, type, null, getCurrentNetworkKey());
	}

	private RequestIpType changeTypeWithNetType(HttpDnsSettings.NetworkDetector networkDetector,
												RequestIpType type) {
		if (type == RequestIpType.auto) {
			if (networkDetector != null) {
				switch (networkDetector.getNetType(mHttpDnsConfig.getContext())) {
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
		CleanHostCacheEvent cleanHostCacheEvent = new CleanHostCacheEvent();
		if (hosts == null || hosts.size() == 0) {
			// 清理所有host
			mResultRepo.clear();
			cleanHostCacheEvent.setTag(ObservableConstants.CLEAN_ALL_HOST_CACHE);
		} else {
			// 清理选中的host
			cleanHostCacheEvent.setTag(ObservableConstants.CLEAN_SPECIFY_HOST_CACHE);
			mResultRepo.clear(hosts);
		}
		cleanHostCacheEvent.setStatusCode(200);
		cleanHostCacheEvent.setCostTime((int) (System.currentTimeMillis() - cleanHostCacheEvent.getTimestamp()));
		mHttpDnsConfig.getObservableManager().addObservableEvent(cleanHostCacheEvent);
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
			return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
		}
		return mResolveHostService.resolveHostSync(host, type, null, getCurrentNetworkKey());
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

		mResolveHostService.resolveHostAsync(host, type, null, getCurrentNetworkKey(), callback);
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
		return mResolveHostService.resolveHostSyncNonBlocking(host, type, null, getCurrentNetworkKey());
	}

	private void tryUpdateRegionServer(Context context, String accountId) {

		if (mHttpDnsConfig.getCurrentServer().shouldUpdateServerIp()) {
			mScheduleService.updateRegionServerIps(Constants.UPDATE_REGION_SERVER_SCENES_INIT);
		} else {
			InitConfig config = InitConfig.getInitConfig(accountId);
			String initRegion = Constants.REGION_DEFAULT;
			if (config != null) {
				initRegion = CommonUtil.fixRegion(config.getRegion());
			}
			SharedPreferences sp = context.getSharedPreferences(
					Constants.CONFIG_CACHE_PREFIX + accountId, Context.MODE_PRIVATE);
			String cachedRegion = sp.getString(Constants.CONFIG_CURRENT_SERVER_REGION,
					Constants.REGION_DEFAULT);

			if (!CommonUtil.regionEquals(cachedRegion, initRegion)) {
				mScheduleService.updateRegionServerIps(Constants.UPDATE_REGION_SERVER_SCENES_REGION_CHANGE);
			}
		}
	}

    /**
     * 智能增量解析：只解析当前网络环境下缺失的域名
     *
     * @param allHosts 所有历史域名
     * @param requestNetworkKey 请求时的网络标识
     */
    private void smartBatchResolve(HashMap<String, RequestIpType> allHosts, String requestNetworkKey) {
        ArrayList<String> v4List = new ArrayList<>();
        ArrayList<String> v6List = new ArrayList<>();
        ArrayList<String> bothList = new ArrayList<>();

        // 检查当前网络环境下是否需要解析
        for (Map.Entry<String, RequestIpType> entry : allHosts.entrySet()) {
            String host = entry.getKey();
            RequestIpType type = entry.getValue();

            // 使用请求时的网络标识检查缓存
            if (needsResolveInNetwork(host, type, requestNetworkKey)) {
                if (type == RequestIpType.v4) {
                    v4List.add(host);
                } else if (type == RequestIpType.v6) {
                    v6List.add(host);
                } else {
                    bothList.add(host);
                }
            }
        }

        // 使用带网络标识的批量解析方法
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
                HttpDnsLog.d("smart resolve " + (v4List.size() + v6List.size() + bothList.size()) + " hosts for network " + requestNetworkKey);
            }
        }
    }

    /**
     * 检查指定网络环境下是否需要解析域名
     *
     * @param host 域名
     * @param type 解析类型
     * @param networkKey 网络标识
     * @return 是否需要解析
     */
    private boolean needsResolveInNetwork(String host, RequestIpType type, String networkKey) {
        // 检查指定网络环境下是否有有效缓存
        ResolveHostCache cache = mResultRepo.getCacheGroup().getCache(networkKey);
        HTTPDNSResultWrapper result = cache.getResult(host, type);
        return result == null || result.isExpired();
    }

    /**
     * 获取当前网络标识，用于网络隔离缓存
     *
     * @return 当前网络标识
     */
    private String getCurrentNetworkKey() {
        return NetworkStateManager.getInstance().getCurrentNetworkKey();
    }
}
