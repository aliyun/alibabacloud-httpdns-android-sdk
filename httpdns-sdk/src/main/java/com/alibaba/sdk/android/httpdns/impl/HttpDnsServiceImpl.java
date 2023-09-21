package com.alibaba.sdk.android.httpdns.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.sdk.android.crashdefend.CrashDefendApi;
import com.alibaba.sdk.android.crashdefend.CrashDefendCallback;
import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.interpret.HostFilter;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostCacheGroup;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostRequestHandler;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResultRepo;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostService;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.content.Context;
import android.os.Looper;

/**
 * 域名解析服务 httpdns接口的实现
 */
public class HttpDnsServiceImpl implements HttpDnsService, ScheduleService.OnServerIpUpdate,
	NetworkStateManager.OnNetworkChange, SyncService {

	protected HttpDnsConfig mHttpDnsConfig;
	private InterpretHostResultRepo mResultRepo;
	protected InterpretHostRequestHandler mRequestHandler;
	protected ScheduleService mScheduleService;
	protected ProbeService mIpProbeService;
	protected InterpretHostService mInterpretHostService;
	protected ResolveHostService mResolveHostService;
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
			mIpProbeService = new ProbeService(this.mHttpDnsConfig);
			mResultRepo = new InterpretHostResultRepo(this.mHttpDnsConfig, this.mIpProbeService,
				new RecordDBHelper(this.mHttpDnsConfig.getContext(), this.mHttpDnsConfig.getAccountId()),
				new InterpretHostCacheGroup());
			mScheduleService = new ScheduleService(this.mHttpDnsConfig, this);
			mRequestHandler = new InterpretHostRequestHandler(mHttpDnsConfig, mScheduleService,
                mSignService);
            HostInterpretRecorder recorder = new HostInterpretRecorder();
            mInterpretHostService = new InterpretHostService(this.mHttpDnsConfig, mIpProbeService,
                mRequestHandler, mResultRepo, mFilter, recorder);
			mResolveHostService = new ResolveHostService(this.mHttpDnsConfig, mResultRepo,
                mRequestHandler,
                mIpProbeService, mFilter, recorder);
			mHttpDnsConfig.setNetworkDetector(HttpDnsNetworkDetector.getInstance());

			beforeInit();

			setupInitConfig(accountId);

			if (mCrashDefendEnabled) {
				initCrashDefend(context, mHttpDnsConfig);
			}
			if (!mHttpDnsConfig.isEnabled()) {
				HttpDnsLog.w("init fail, crashdefend");
				return;
			}
			NetworkStateManager.getInstance().init(context);
			NetworkStateManager.getInstance().addListener(this);
			if (mHttpDnsConfig.getCurrentServer().shouldUpdateServerIp()
				|| !mHttpDnsConfig.isCurrentRegionMatch()) {
				mScheduleService.updateServerIps();
			}
			favorInit(context, accountId);

			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("httpdns service is inited " + accountId);
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
			setExpiredIPEnabled(config.isEnableExpiredIp());
			if (config.getIpProbeItems() != null) {
				setIPProbeList(config.getIpProbeItems());
			}
			// 设置region 必须在 读取缓存之前
			if (config.getRegion() != InitConfig.NOT_SET) {
				this.mHttpDnsConfig.setRegion(config.getRegion());
			}
			// 设置 主站域名 需要在 读取缓存之前
			this.mResultRepo.setHostListWhichIpFixed(config.getHostListWithFixedIp());
			// 设置缓存控制，并读取缓存
			setCachedIPEnabled(config.isEnableCacheIp());
			this.mResultRepo.setCacheTtlChanger(config.getCacheTtlChanger());
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
	public void setLogEnabled(boolean shouldPrintLog) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		HttpDnsLog.enable(shouldPrintLog);
	}

	@Override
	public void setPreResolveHosts(ArrayList<String> hostList) {
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
	public void setPreResolveHosts(ArrayList<String> hostList, RequestIpType requestIpType) {
		if (!mHttpDnsConfig.isEnabled()) {
			HttpDnsLog.i("service is disabled");
			return;
		}
		if (hostList == null || hostList.size() == 0) {
			HttpDnsLog.i("setPreResolveHosts empty list");
			return;
		}
		requestIpType = changeTypeWithNetType(mHttpDnsConfig.getNetworkDetector(), requestIpType);
		mResolveHostService.resolveHostAsync(hostList, requestIpType);
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
		String[] ips = getIpsByHostAsync(host);
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
		return mInterpretHostService.interpretHostAsync(host, RequestIpType.v4, null, null).getIps();
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
		return mInterpretHostService.interpretHostAsync(host, RequestIpType.v6, null, null)
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
		return mInterpretHostService.interpretHostAsync(host, RequestIpType.both, null, null);
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
		return mInterpretHostService.interpretHostAsync(host, type, null, null);
	}

	@Override
	public void setExpiredIPEnabled(boolean enable) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mInterpretHostService.setEnableExpiredIp(enable);
	}

	@Override
	public void setCachedIPEnabled(boolean enable) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		setCachedIPEnabled(enable, false);
	}

	@Override
	public void setCachedIPEnabled(boolean enable, boolean autoCleanCacheAfterLoad) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mResultRepo.setCachedIPEnabled(enable, autoCleanCacheAfterLoad);
	}

	@Override
	public void setAuthCurrentTime(long time) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mSignService.setCurrentTimestamp(time);
	}

	@Override
	public void setDegradationFilter(DegradationFilter filter) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		this.mFilter.setFilter(filter);
	}

	@Override
	public void setPreResolveAfterNetworkChanged(boolean enable) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		this.resolveAfterNetworkChange = enable;
	}

	@Override
	public void setTimeoutInterval(int timeoutInterval) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		this.mHttpDnsConfig.setTimeout(timeoutInterval);
	}

	@Override
	public void setHTTPSRequestEnabled(boolean enabled) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		boolean changed = mHttpDnsConfig.setHTTPSRequestEnabled(enabled);
		if (changed && enabled) {
			// 避免应用禁止http请求，导致初始化时的服务更新请求失败
			if (mHttpDnsConfig.getCurrentServer().shouldUpdateServerIp()) {
				mScheduleService.updateServerIps();
			}
		}
	}

	@Override
	public void setIPProbeList(List<IPProbeItem> ipProbeList) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mIpProbeService.setIPProbeItems(ipProbeList);
	}

	@Override
	public String getSessionId() {
		return SessionTrackMgr.getInstance().getSessionId();
	}

	@Override
	public void setLogger(ILogger logger) {
		HttpDnsLog.setLogger(logger);
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
		return mInterpretHostService.interpretHostAsync(host, RequestIpType.v4, params, cacheKey);
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
		return mInterpretHostService.interpretHostAsync(host, type, params, cacheKey);
	}

	@Override
	public void setSdnsGlobalParams(Map<String, String> params) {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mRequestHandler.setSdnsGlobalParams(params);
	}

	@Override
	public void clearSdnsGlobalParams() {
		if (!mHttpDnsConfig.isEnabled()) {
			return;
		}
		mRequestHandler.clearSdnsGlobalParams();
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
		mScheduleService.updateServerIps(region);
	}

	@Override
	public void enableIPv6(boolean enable) {
		// deprecated
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
							mResolveHostService.resolveHostAsync(v4List, RequestIpType.v4);
						}
						if (v6List.size() > 0) {
							mResolveHostService.resolveHostAsync(v6List, RequestIpType.v6);
						}
						if (bothList.size() > 0) {
							mResolveHostService.resolveHostAsync(bothList, RequestIpType.both);
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
			return mInterpretHostService.interpretHostAsync(host, type, null, null);
		}
		return mInterpretHostService.interpretHost(host, type, null, null);
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
    public void enableCrashDefend(boolean enabled) {
        mCrashDefendEnabled = enabled;
    }
}
