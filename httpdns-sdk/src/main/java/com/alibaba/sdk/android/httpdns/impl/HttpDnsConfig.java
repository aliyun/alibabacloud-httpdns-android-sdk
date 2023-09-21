package com.alibaba.sdk.android.httpdns.impl;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.config.ConfigCacheHelper;
import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.config.ServerConfig;
import com.alibaba.sdk.android.httpdns.config.SpCacheItem;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * httpdns的配置
 */
public class HttpDnsConfig implements SpCacheItem {
	private final Context mContext;
	private boolean mEnabled = Constants.DEFAULT_SDK_ENABLE;
	/**
	 * 初始服务节点
	 */
	private final RegionServer mInitServer = new RegionServer(BuildConfig.INIT_SERVER,
		Constants.NO_PORTS,
		BuildConfig.IPV6_INIT_SERVER, Constants.NO_PORTS, Constants.REGION_DEFAULT);

	/**
	 * 兜底的调度服务IP，用于应对国际版服务IP有可能不稳定的情况
	 */
	private final RegionServer mDefaultUpdateServer = new RegionServer(BuildConfig.UPDATE_SERVER,
		Constants.NO_PORTS, BuildConfig.IPV6_UPDATE_SERVER, Constants.NO_PORTS,
		Constants.REGION_DEFAULT);

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
	private String mRegion = Constants.REGION_DEFAULT;
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
	private boolean mProbeDisabled = false;

	/**
	 * 网络探测接口
	 */
	private HttpDnsSettings.NetworkDetector mNetworkDetector = null;

	private final ConfigCacheHelper mCacheHelper;

	protected ExecutorService mWorker = ThreadUtil.createExecutorService();
	protected ExecutorService mDbWorker = ThreadUtil.createDBExecutorService();

	public HttpDnsConfig(Context context, String accountId) {
		this.mContext = context;
		this.mAccountId = accountId;
		this.mCurrentServer = new ServerConfig(this);
		// 先从缓存读取数据，再赋值cacheHelper， 避免在读取缓存过程中，触发写缓存操作
		ConfigCacheHelper helper = new ConfigCacheHelper();
		helper.restoreFromCache(context, this);
		this.mCacheHelper = helper;
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

	public String getSchema() {
		return mSchema;
	}

	public ExecutorService getWorker() {
		return mWorker;
	}

	public ExecutorService getDbWorker() {
		return mDbWorker;
	}

	public void setWorker(ExecutorService worker) {
		// 给测试使用
		this.mWorker = worker;
		this.mDbWorker = worker;
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

	/**
	 * 设置用户切换的region
	 */
	public boolean setRegion(String region) {
		if (!this.mRegion.equals(region)) {
			this.mRegion = region;
			saveToCache();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		HttpDnsConfig that = (HttpDnsConfig)o;
		return mEnabled == that.mEnabled &&
			mTimeout == that.mTimeout &&
			mHitCrashDefend == that.mHitCrashDefend &&
			mRemoteDisabled == that.mRemoteDisabled &&
			mProbeDisabled == that.mProbeDisabled &&
			CommonUtil.equals(mContext, that.mContext) &&
			CommonUtil.equals(mInitServer, that.mInitServer) &&
			CommonUtil.equals(mDefaultUpdateServer, that.mDefaultUpdateServer) &&
			CommonUtil.equals(mCurrentServer, that.mCurrentServer) &&
			CommonUtil.equals(mAccountId, that.mAccountId) &&
			CommonUtil.equals(mSchema, that.mSchema) &&
			CommonUtil.equals(mRegion, that.mRegion) &&
			CommonUtil.equals(mCacheHelper, that.mCacheHelper) &&
			CommonUtil.equals(mWorker, that.mWorker) &&
			CommonUtil.equals(mDbWorker, that.mDbWorker);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(
			new Object[] {mContext, mEnabled, mInitServer, mDefaultUpdateServer, mCurrentServer,
				mAccountId, mSchema, mRegion, mTimeout, mHitCrashDefend, mRemoteDisabled,
				mProbeDisabled,
				mCacheHelper, mWorker, mDbWorker});
	}

	/**
	 * 设置初始服务IP
	 * <p>
	 * 线上SDK 初始化服务IP是内置写死的。
	 * 本API主要用于一些测试代码使用
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

	public void probeDisable(boolean disable) {
		this.mProbeDisabled = disable;
	}

	public boolean isProbeDisabled() {
		return mProbeDisabled;
	}

	public HttpDnsSettings.NetworkDetector getNetworkDetector() {
		return mNetworkDetector;
	}

	public void setNetworkDetector(HttpDnsSettings.NetworkDetector networkDetector) {
		this.mNetworkDetector = networkDetector;
	}

	// 缓存相关的 处理，暂时放这里

	public void saveToCache() {
		if (mCacheHelper != null) {
			mCacheHelper.saveConfigToCache(mContext, this);
		}
	}

	public SpCacheItem[] getCacheItem() {
		return new SpCacheItem[] {this, mCurrentServer};
	}

	@Override
	public void restoreFromCache(SharedPreferences sp) {
		mEnabled = sp.getBoolean(Constants.CONFIG_ENABLE, Constants.DEFAULT_SDK_ENABLE);
	}

	@Override
	public void saveToCache(SharedPreferences.Editor editor) {
		editor.putBoolean(Constants.CONFIG_ENABLE, mEnabled);
	}
}
