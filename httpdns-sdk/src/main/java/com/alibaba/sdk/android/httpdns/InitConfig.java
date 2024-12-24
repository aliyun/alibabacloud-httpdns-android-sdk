package com.alibaba.sdk.android.httpdns;

import android.text.format.DateUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 初始化配置
 * 之前的初始化方式，每个配置都是单独设置的，有可能造成一些时序上的问题
 * 所以增加统一初始化配置的方法，由内部固定初始化逻辑，避免时序问题
 */
public class InitConfig {

	private static final Map<String, InitConfig> configs = new ConcurrentHashMap<>();

	static void addConfig(String accountId, InitConfig config) {
		configs.put(accountId, config);
	}

	public static final String NOT_SET = null;

	public static InitConfig getInitConfig(String accountId) {
		return configs.get(accountId);
	}

	public static void removeConfig(String accountId) {
		if (accountId == null || accountId.isEmpty()) {
			configs.clear();
		} else {
			configs.remove(accountId);
		}
	}

	private final boolean mEnableExpiredIp;
	private final boolean mEnableCacheIp;
	private final long mExpiredThresholdMillis;
	private final int mTimeout;
	private final boolean mEnableHttps;
	private final List<IPRankingBean> mIPRankingList;
	private final String mRegion;
	private final CacheTtlChanger mCacheTtlChanger;
	private final List<String> mHostListWithFixedIp;
	private final boolean mResolveAfterNetworkChange;
	private final DegradationFilter mDegradationFilter;
	private final NotUseHttpDnsFilter mNotUseHttpDnsFilter;
	private final boolean mEnableCrashDefend;
	private final Map<String, String> mSdnsGlobalParams;
	private final boolean mEnableDegradationLocalDns;

	private InitConfig(Builder builder) {
		mEnableExpiredIp = builder.enableExpiredIp;
		mEnableCacheIp = builder.enableCacheIp;
		mExpiredThresholdMillis = builder.expiredThresholdMillis;
		mTimeout = builder.timeout;
		mEnableHttps = builder.enableHttps;
		mIPRankingList = builder.ipRankingList;
		mRegion = builder.region;
		mCacheTtlChanger = builder.cacheTtlChanger;
		mHostListWithFixedIp = builder.hostListWithFixedIp;
		mResolveAfterNetworkChange = builder.resolveAfterNetworkChange;
		mDegradationFilter = builder.degradationFilter;
		mNotUseHttpDnsFilter = builder.notUseHttpDnsFilter;
		mEnableCrashDefend = builder.enableCrashDefend;
		mSdnsGlobalParams = builder.sdnsGlobalParams;
		mEnableDegradationLocalDns = builder.enableDegradationLocalDns;
	}

	public boolean isEnableExpiredIp() {
		return mEnableExpiredIp;
	}

	public boolean isEnableCacheIp() {
		return mEnableCacheIp;
	}

	public long getExpiredThresholdMillis() {
		return mExpiredThresholdMillis;
	}

	public boolean isResolveAfterNetworkChange() {
		return mResolveAfterNetworkChange;
	}

	public int getTimeout() {
		return mTimeout;
	}

	public boolean isEnableHttps() {
		return mEnableHttps;
	}

	public boolean isEnableDegradationLocalDns() {
		return mEnableDegradationLocalDns;
	}

	public List<IPRankingBean> getIPRankingList() {
		return mIPRankingList;
	}

	public String getRegion() {
		return mRegion;
	}

	public CacheTtlChanger getCacheTtlChanger() {
		return mCacheTtlChanger;
	}

	public List<String> getHostListWithFixedIp() {
		return mHostListWithFixedIp;
	}

	@Deprecated
	public DegradationFilter getDegradationFilter() {
		return mDegradationFilter;
	}

	public NotUseHttpDnsFilter getNotUseHttpDnsFilter() {
		return mNotUseHttpDnsFilter;
	}

	public boolean isEnableCrashDefend() {
		return mEnableCrashDefend;
	}

	public Map<String, String> getSdnsGlobalParams() {
		return mSdnsGlobalParams;
	}

	public static class Builder {
		private boolean enableExpiredIp = Constants.DEFAULT_ENABLE_EXPIRE_IP;
		private boolean enableCacheIp = Constants.DEFAULT_ENABLE_CACHE_IP;
		private long expiredThresholdMillis = 0L;
		private int timeout = Constants.DEFAULT_TIMEOUT;
		private boolean enableDegradationLocalDns = Constants.DEFAULT_ENABLE_DEGRADATION_LOCAL_DNS;
		private boolean enableHttps = Constants.DEFAULT_ENABLE_HTTPS;
		private List<IPRankingBean> ipRankingList = null;
		private String region = NOT_SET;
		private CacheTtlChanger cacheTtlChanger = null;
		private List<String> hostListWithFixedIp = null;
		private boolean resolveAfterNetworkChange = true;
		private DegradationFilter degradationFilter = null;
		private NotUseHttpDnsFilter notUseHttpDnsFilter = null;
		private boolean enableCrashDefend = false;
		private Map<String, String> sdnsGlobalParams = null;

		/**
		 * 设置是否允许返回超过ttl 的ip
		 * @param enableExpiredIp 是否允许返回超过ttl 的ip
		 * @return {@link Builder}
		 */
		public Builder setEnableExpiredIp(boolean enableExpiredIp) {
			this.enableExpiredIp = enableExpiredIp;
			return this;
		}

		/**
		 * 设置是否允许使用DB缓存，默认不允许
		 * @param enableCacheIp 是否允许使用DB缓存
		 * @return {@link Builder}
		 */
		public Builder setEnableCacheIp(boolean enableCacheIp) {
			this.enableCacheIp = enableCacheIp;
			return this;
		}

		public Builder setEnableCacheIp(boolean enableCacheIp, long expiredThresholdMillis) {
			this.enableCacheIp = enableCacheIp;
			if (expiredThresholdMillis >= 0 && expiredThresholdMillis <= DateUtils.YEAR_IN_MILLIS) {
				this.expiredThresholdMillis = expiredThresholdMillis;
			}

			return this;
		}

		/**
		 * 设置请求超时时间,单位ms,默认为2s
		 * @param timeoutMillis 超时时间，单位ms
		 * @return {@link Builder}
		 */
		public Builder setTimeoutMillis(int timeoutMillis) {
			timeout = timeoutMillis;
			return this;
		}

		/**
		 * 设置请求超时时间,单位ms,默认为2s
		 * @param timeout 超时时间，单位ms
		 * @return {@link Builder}
		 */
		@Deprecated
		public Builder setTimeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * 设置开启/关闭降级到Local Dns，在httpdns解析失败或者域名被过滤不走httpdns的时候，开启降级会走local dns解析
		 * @param enableDegradation true, 开启 ｜ false, 关闭
		 * @return {@link Builder}
		 */
		public Builder setEnableDegradationLocalDns(boolean enableDegradation) {
			enableDegradationLocalDns = enableDegradation;
			return this;
		}

		/**
		 * 设置HTTPDNS域名解析请求类型(HTTP/HTTPS)，若不调用该接口，默认为HTTP请求
		 * @param enableHttps 是否使用https
		 * @return {@link Builder}
		 */
		public Builder setEnableHttps(boolean enableHttps) {
			this.enableHttps = enableHttps;
			return this;
		}

		/**
		 * 设置要探测的域名列表,默认只会对ipv4的地址进行ip优选
		 * @param ipRankingList {@link IPRankingBean}
		 * @return {@link Builder}
		 */
		public Builder setIPRankingList(List<IPRankingBean> ipRankingList) {
			this.ipRankingList = ipRankingList;
			return this;
		}

		@Deprecated
		public Builder setRegion(String region) {
			this.region = region;
			return this;
		}

		public Builder setRegion(Region region) {
			if (region != null) {
				this.region = region.getRegion();
			}

			return this;
		}

		/**
		 * 配置自定义ttl的逻辑
		 *
		 * @param cacheTtlChanger 修改ttl的接口
		 */
		public Builder configCacheTtlChanger(CacheTtlChanger cacheTtlChanger) {
			this.cacheTtlChanger = cacheTtlChanger;
			return this;
		}

		/**
		 * 配置主站域名列表
		 *
		 * @param hostListWithFixedIp 主站域名列表
		 */
		public Builder configHostWithFixedIp(List<String> hostListWithFixedIp) {
			this.hostListWithFixedIp = hostListWithFixedIp;
			return this;
		}

		/**
		 * 设置网络切换时是否自动刷新所有域名解析结果，默认自动刷新
		 * @param enable 是否允许自动刷新域名解析结果
		 * @return {@link Builder}
		 */
		public Builder setPreResolveAfterNetworkChanged(boolean enable) {
			resolveAfterNetworkChange = enable;
			return this;
		}

		/**
		 * 设置降级策略, 用户可定制规则降级为原生DNS解析方式
		 * @param filter {@link  DegradationFilter}
		 * @return {@link Builder}
		 */
		@Deprecated
		public Builder setDegradationFilter(DegradationFilter filter) {
			degradationFilter = filter;
			return this;
		}

		/**
		 * 设置不使用HttpDns的策略, 用户可定制规则指定不走httpdns的域名
		 * @param filter {@link  NotUseHttpDnsFilter}
		 * @return {@link Builder}
		 */
		public Builder setNotUseHttpDnsFilter(NotUseHttpDnsFilter filter) {
			notUseHttpDnsFilter = filter;
			return this;
		}

		/**
		 * 是否开启sdk内部的崩溃保护机制，默认是关闭的
		 * @param enabled 开启/关闭
		 * @return {@link Builder}
		 */
		public Builder enableCrashDefend(boolean enabled) {
			enableCrashDefend = enabled;
			return this;
		}

		/**
		 * 设置sdns全局参数（该全局参数不影响异步解析任务，只用于解析接口调用时进行参数合并）
		 * @param params sdn的全局参数
		 * @return {@link Builder}
		 */
		public Builder setSdnsGlobalParams(Map<String, String> params) {
			sdnsGlobalParams = params;
			return this;
		}

		public InitConfig build() {
			return new InitConfig(this);
		}

		public InitConfig buildFor(String accountId) {
			InitConfig config = new InitConfig(this);
			addConfig(accountId, config);
			return config;
		}
	}
}
