package com.alibaba.sdk.android.httpdns.impl;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.config.ConfigCacheHelper;
import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.config.ServerConfig;
import com.alibaba.sdk.android.httpdns.config.region.RegionServerManager;
import com.alibaba.sdk.android.httpdns.config.SpCacheItem;
import com.alibaba.sdk.android.httpdns.observable.ObservableConfig;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;
import com.alibaba.sdk.android.httpdns.observable.ObservableManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * httpdns的配置
 */
public class HttpDnsConfig implements SpCacheItem {
	private final Context mContext;
	private boolean mEnabled = Constants.DEFAULT_SDK_ENABLE;
	/**
	 * 初始服务节点
	 */
	private RegionServer mInitServer;

	/**
	 * 兜底的调度服务IP，用于应对国际版服务IP有可能不稳定的情况
	 */
	private RegionServer mDefaultUpdateServer;

	/**
	 * 当前服务节点
	 */
	private final ServerConfig mCurrentServer;

	/**
	 * 用户的accountId
	 */
	private final String mAccountId;
	/**
	 * 当前请求使用的schema
	 */
	private String mSchema = Constants.DEFAULT_SCHEMA;
	/**
	 * 当前region
	 */
	private String mRegion;
	/**
	 * 超时时长
	 */
	private int mTimeout = Constants.DEFAULT_TIMEOUT;

	/**
	 * 是否禁用服务，以避免崩溃
	 */
	private boolean mHitCrashDefend;
	/**
	 * 是否远程禁用服务
	 */
	private boolean mRemoteDisabled = false;
	/**
	 * 是否禁用probe能力
	 */
	private boolean mIPRankingDisabled = false;
	/**
	 * 是否开启降级到Local Dns
	 */
	private boolean mEnableDegradationLocalDns = Constants.DEFAULT_ENABLE_DEGRADATION_LOCAL_DNS;

	/**
	 * 网络探测接口
	 */
	private HttpDnsSettings.NetworkDetector mNetworkDetector = null;

	private final ConfigCacheHelper mCacheHelper;

	protected ExecutorService mWorker = ThreadUtil.createExecutorService();
	protected ExecutorService mResolveWorker = ThreadUtil.createResolveExecutorService();
	protected ExecutorService mDbWorker = ThreadUtil.createDBExecutorService();
	private ObservableConfig mObservableConfig;
	private ObservableManager mObservableManager;
	private float mObservableSampleBenchMarks;
	private String mBizTags;
	private String mUA;

	public HttpDnsConfig(Context context, String accountId, String secret) {
		mContext = context;
		mAccountId = accountId;
		if (mContext != null) {
			mUA = buildUA();
		}

		//region提前设置
		mRegion = getInitRegion(accountId);
		mInitServer = RegionServerManager.getInitServer(mRegion);
		mDefaultUpdateServer = RegionServerManager.getUpdateServer(mRegion);

		mCurrentServer = new ServerConfig(this, mInitServer.getServerIps(), mInitServer.getPorts(), mInitServer.getIpv6ServerIps(), mInitServer.getIpv6Ports());
		mObservableConfig = new ObservableConfig();
		// 先从缓存读取数据，再赋值cacheHelper， 避免在读取缓存过程中，触发写缓存操作
		ConfigCacheHelper helper = new ConfigCacheHelper();
		if (context != null) {
			helper.restoreFromCache(context, this);
		}
		mCacheHelper = helper;

		mObservableManager = new ObservableManager(this, secret);
	}

	public Context getContext() {
		return mContext;
	}

	public String getAccountId() {
		return mAccountId;
	}

	public ServerConfig getCurrentServer() {
		return mCurrentServer;
	}

	public ObservableConfig getObservableConfig() {
		return mObservableConfig;
	}

	public ObservableManager getObservableManager() {
		return mObservableManager;
	}

	public float getObservableSampleBenchMarks() {
		return mObservableSampleBenchMarks;
	}

	public void setObservableSampleBenchMarks(float benchMarks) {
		if (mObservableSampleBenchMarks != benchMarks) {
			mObservableSampleBenchMarks = benchMarks;
			saveToCache();
		}
	}

	public boolean isCurrentRegionMatch() {
		return CommonUtil.regionEquals(mRegion, mCurrentServer.getRegion());
	}

	public boolean isAllInitServer() {
		return mInitServer.serverEquals((RegionServer)mCurrentServer);
	}

	public String getRegion() {
		return mRegion;
	}

	public boolean isEnabled() {
		return mEnabled && !mHitCrashDefend && !mRemoteDisabled;
	}

	/**
	 * 是否启用httpdns
	 * <p>
	 * 注意是 永久禁用，因为缓存的原因，一旦禁用，就没有机会启用了
	 */
	public void setEnabled(boolean enabled) {
		if (this.mEnabled != enabled) {
			this.mEnabled = enabled;
			saveToCache();
		}
	}

	public int getTimeout() {
		return mTimeout;
	}

	public void setTimeout(int timeout) {
		if (this.mTimeout != timeout) {
			this.mTimeout = timeout;
			saveToCache();
		}
	}

	public String getBizTags() {
		return mBizTags;
	}

	public void setBizTags(String tags) {
		mBizTags = tags;
	}

	public String getSchema() {
		return mSchema;
	}

	public ExecutorService getWorker() {
		return mWorker;
	}

	public ExecutorService getResolveWorker() {
		return mResolveWorker;
	}

	public ExecutorService getDbWorker() {
		return mDbWorker;
	}

	public void setWorker(ExecutorService worker) {
		// 给测试使用
		this.mWorker = worker;
		this.mDbWorker = worker;
		mResolveWorker = worker;
	}

	/**
	 * 切换https
	 *
	 * @return 配置是否变化
	 */
	public boolean setHTTPSRequestEnabled(boolean enabled) {
		String oldSchema = mSchema;
		if (enabled) {
			mSchema = HttpRequestConfig.HTTPS_SCHEMA;
		} else {
			mSchema = HttpRequestConfig.HTTP_SCHEMA;
		}
		if (!mSchema.equals(oldSchema)) {
			saveToCache();
		}
		return !mSchema.equals(oldSchema);
	}

	public void setEnableDegradationLocalDns(boolean enable) {
		mEnableDegradationLocalDns = enable;
	}

	public boolean isEnableDegradationLocalDns() {
		return mEnableDegradationLocalDns;
	}

	/**
	 * 设置用户切换的region
	 */
	public boolean setRegion(String region) {
		if (!mRegion.equals(region)) {
			mRegion = region;
			mInitServer = RegionServerManager.getInitServer(mRegion);
			mDefaultUpdateServer = RegionServerManager.getUpdateServer(mRegion);
			mCurrentServer.setServerIps(mRegion, mInitServer.getServerIps(), mInitServer.getPorts(), mInitServer.getIpv6ServerIps(), mInitServer.getIpv6Ports());
			return true;
		}
		return false;
	}

	/**
	 * 获取ipv6的服务节点
	 */
	public String[] getIpv6ServerIps() {
		return this.mCurrentServer.getIpv6ServerIps();
	}

	public RegionServer getInitServer() {
		return this.mInitServer;
	}

	public RegionServer getDefaultUpdateServer() {
		return mDefaultUpdateServer;
	}

	public String[] getDefaultIpv6UpdateServer() {
		return mDefaultUpdateServer.getIpv6ServerIps();
	}

	public String getUA() {
		return mUA;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		HttpDnsConfig that = (HttpDnsConfig)o;
		return mEnabled == that.mEnabled &&
			mTimeout == that.mTimeout &&
			mHitCrashDefend == that.mHitCrashDefend &&
			mRemoteDisabled == that.mRemoteDisabled &&
			mIPRankingDisabled == that.mIPRankingDisabled &&
			CommonUtil.equals(mContext, that.mContext) &&
			CommonUtil.equals(mInitServer, that.mInitServer) &&
			CommonUtil.equals(mDefaultUpdateServer, that.mDefaultUpdateServer) &&
			CommonUtil.equals(mCurrentServer, that.mCurrentServer) &&
			CommonUtil.equals(mAccountId, that.mAccountId) &&
			CommonUtil.equals(mSchema, that.mSchema) &&
			CommonUtil.equals(mRegion, that.mRegion) &&
			CommonUtil.equals(mCacheHelper, that.mCacheHelper) &&
			CommonUtil.equals(mWorker, that.mWorker) &&
			CommonUtil.equals(mResolveWorker, that.mResolveWorker) &&
			CommonUtil.equals(mDbWorker, that.mDbWorker);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(
			new Object[] {mContext, mEnabled, mInitServer, mDefaultUpdateServer, mCurrentServer,
				mAccountId, mSchema, mRegion, mTimeout, mHitCrashDefend, mRemoteDisabled,
				mIPRankingDisabled,
				mCacheHelper, mWorker, mResolveWorker, mDbWorker});
	}

	/**
	 * 设置初始服务IP
	 * <p>
	 * 线上SDK 初始化服务IP是内置写死的。
	 * 本API主要用于一些测试代码使用
	 *
	 */
	public void setInitServers(String initRegion, String[] initIps, int[] initPorts,
							   String[] initIpv6s, int[] initV6Ports) {
		this.mRegion = initRegion;
		if (initIps == null) {
			return;
		}
		String[] oldInitServerIps = this.mInitServer.getServerIps();
		int[] oldInitPorts = this.mInitServer.getPorts();
		String[] oldInitIpv6ServerIps = this.mInitServer.getIpv6ServerIps();
		int[] oldInitIpv6Ports = this.mInitServer.getIpv6Ports();
		this.mInitServer.updateAll(initRegion, initIps, initPorts, initIpv6s, initV6Ports);
		if (mCurrentServer.getServerIps() == null
			|| mCurrentServer.getIpv6ServerIps() == null
			|| (CommonUtil.isSameServer(oldInitServerIps, oldInitPorts,
			mCurrentServer.getServerIps(), mCurrentServer.getPorts())
			&& CommonUtil.isSameServer(oldInitIpv6ServerIps, oldInitIpv6Ports,
			mCurrentServer.getIpv6ServerIps(), mCurrentServer.getIpv6Ports()))) {
			mCurrentServer.setServerIps(initRegion, initIps, initPorts, initIpv6s, initV6Ports);
		}
	}

	/**
	 * 设置兜底的调度IP，
	 * 测试代码使用
	 */
	public void setDefaultUpdateServer(String[] ips, int[] ports) {
		this.mDefaultUpdateServer.updateRegionAndIpv4(this.mInitServer.getRegion(), ips, ports);
	}

	public void setDefaultUpdateServerIpv6(String[] defaultServerIps, int[] ports) {
		this.mDefaultUpdateServer.updateIpv6(defaultServerIps, ports);
	}

	public void crashDefend(boolean crashDefend) {
		this.mHitCrashDefend = crashDefend;
	}

	public void remoteDisable(boolean disable) {
		this.mRemoteDisabled = disable;
	}

	public void ipRankingDisable(boolean disable) {
		this.mIPRankingDisabled = disable;
	}

	public boolean isIPRankingDisabled() {
		return mIPRankingDisabled;
	}

	public HttpDnsSettings.NetworkDetector getNetworkDetector() {
		return mNetworkDetector;
	}

	public void setNetworkDetector(HttpDnsSettings.NetworkDetector networkDetector) {
		this.mNetworkDetector = networkDetector;
	}

	// 缓存相关的 处理，暂时放这里

	public void saveToCache() {
		if (mCacheHelper != null && mContext != null) {
			mCacheHelper.saveConfigToCache(mContext, this);
		}
	}

	public SpCacheItem[] getCacheItem() {
		return new SpCacheItem[] {this, mCurrentServer, mObservableConfig};
	}

	@Override
	public void restoreFromCache(SharedPreferences sp) {
		mEnabled = sp.getBoolean(Constants.CONFIG_ENABLE, Constants.DEFAULT_SDK_ENABLE);
		mObservableSampleBenchMarks = sp.getFloat(Constants.CONFIG_OBSERVABLE_BENCH_MARKS, -1);
	}

	@Override
	public void saveToCache(SharedPreferences.Editor editor) {
		editor.putBoolean(Constants.CONFIG_ENABLE, mEnabled);
		editor.putFloat(Constants.CONFIG_OBSERVABLE_BENCH_MARKS, mObservableSampleBenchMarks);
	}

	private String getInitRegion(String accountId) {
		InitConfig config = InitConfig.getInitConfig(accountId);
		if (config == null) {
			return Constants.REGION_DEFAULT;
		}

		return CommonUtil.fixRegion(config.getRegion());
	}

	private String buildUA() {
		if (mContext == null) {
			return "";
		}
		String versionName = ObservableConstants.UNKNOWN;
		try {
			versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {

		}

		return Build.BRAND + "/" + Build.MODEL
				+ ";" + "Android" + "/" + Build.VERSION.RELEASE
				+ ";" + mContext.getPackageName() + "/" + versionName
				+ ";" + "HTTPDNS" + "/" + BuildConfig.VERSION_NAME;
	}
}
