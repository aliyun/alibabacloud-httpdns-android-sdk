package com.alibaba.sdk.android.httpdns;

import java.util.HashMap;
import java.util.List;

import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 初始化配置
 * 之前的初始化方式，每个配置都是单独设置的，有可能造成一些时序上的问题
 * 所以增加统一初始化配置的方法，由内部固定初始化逻辑，避免时序问题
 */
public class InitConfig {

	private static final HashMap<String, InitConfig> configs = new HashMap<>();

	private static void addConfig(String accountId, InitConfig config) {
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
	private final int mTimeout;
	private final boolean mEnableHttps;
	private final List<IPProbeItem> mIpProbeItems;
	private final String mRegion;
	private final CacheTtlChanger mCacheTtlChanger;
	private final List<String> mHostListWithFixedIp;

	private InitConfig(Builder builder) {
		mEnableExpiredIp = builder.enableExpiredIp;
		mEnableCacheIp = builder.enableCacheIp;
		mTimeout = builder.timeout;
		mEnableHttps = builder.enableHttps;
		mIpProbeItems = builder.ipProbeItems;
		mRegion = builder.region;
		mCacheTtlChanger = builder.cacheTtlChanger;
		mHostListWithFixedIp = builder.hostListWithFixedIp;
	}

	public boolean isEnableExpiredIp() {
		return mEnableExpiredIp;
	}

	public boolean isEnableCacheIp() {
		return mEnableCacheIp;
	}

	public int getTimeout() {
		return mTimeout;
	}

	public boolean isEnableHttps() {
		return mEnableHttps;
	}

	public List<IPProbeItem> getIpProbeItems() {
		return mIpProbeItems;
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

	public static class Builder {
		private boolean enableExpiredIp = Constants.DEFAULT_ENABLE_EXPIRE_IP;
		private boolean enableCacheIp = Constants.DEFAULT_ENABLE_CACHE_IP;
		private int timeout = Constants.DEFAULT_TIMEOUT;
		private boolean enableHttps = Constants.DEFAULT_ENABLE_HTTPS;
		private List<IPProbeItem> ipProbeItems = null;
		private String region = NOT_SET;
		private CacheTtlChanger cacheTtlChanger = null;
		private List<String> hostListWithFixedIp = null;

		public Builder setEnableExpiredIp(boolean enableExpiredIp) {
			this.enableExpiredIp = enableExpiredIp;
			return this;
		}

		public Builder setEnableCacheIp(boolean enableCacheIp) {
			this.enableCacheIp = enableCacheIp;
			return this;
		}

		public Builder setTimeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder setEnableHttps(boolean enableHttps) {
			this.enableHttps = enableHttps;
			return this;
		}

		public Builder setIpProbeItems(List<IPProbeItem> ipProbeItems) {
			this.ipProbeItems = ipProbeItems;
			return this;
		}

		public Builder setRegion(String region) {
			this.region = region;
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

		public InitConfig buildFor(String accountId) {
			InitConfig config = new InitConfig(this);
			addConfig(accountId, config);
			return config;
		}
	}
}
