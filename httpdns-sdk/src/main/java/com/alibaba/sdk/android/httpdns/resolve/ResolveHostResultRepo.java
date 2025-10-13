package com.alibaba.sdk.android.httpdns.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HTTPDNSResultWrapper;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingCallback;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;

/**
 * 域名解析结果
 */
public class ResolveHostResultRepo {

	private final RecordDBHelper mDBHelper;
	private boolean mEnableCache = false;
	private final HttpDnsConfig mHttpDnsConfig;
	private final IPRankingService mIpIPRankingService;
	private final ResolveHostCacheGroup mCacheGroup;
	private CacheTtlChanger mCacheTtlChanger;
	private final ArrayList<String> mHostListWhichIpFixed = new ArrayList<>();
	private volatile boolean mFinishReadFromDb = false;
	private final Object mDbLock = new Object();
	private long mExpiredThresholdMillis = 0L;

	public ResolveHostResultRepo(HttpDnsConfig config, IPRankingService ipIPRankingService,
								 RecordDBHelper dbHelper, ResolveHostCacheGroup cacheGroup) {
		this.mHttpDnsConfig = config;
		this.mIpIPRankingService = ipIPRankingService;
		this.mDBHelper = dbHelper;
		this.mCacheGroup = cacheGroup;
	}

	public void ensureReadFromDB() {
		if (!mEnableCache) {
			return;
		}

		if (mFinishReadFromDb) {
			return;
		}

		synchronized (mDbLock) {
			if (mFinishReadFromDb) {
				return;
			}

			readFromDB(mExpiredThresholdMillis);
			mFinishReadFromDb = true;
		}
	}

	private void readFromDB(long expiredThresholdMillis) {
		final String region = mHttpDnsConfig.getRegion();
		List<HostRecord> records = mDBHelper.readFromDb(region);
		for (HostRecord record : records) {

			// 过期缓存不加载到内存中
			if (System.currentTimeMillis() >= record.getQueryTime() + record.getTtl() * 1000L + expiredThresholdMillis) {
				continue;
			}

			if (record.getIps() == null || record.getIps().length == 0) {
				// 空解析，按照ttl来，不区分是否来自数据库
				record.setFromDB(false);
			}
			if (mHostListWhichIpFixed.contains(record.getHost())) {
				// 固定IP，按照ttl来，不区分是否来自数据库
				record.setFromDB(false);
			}
			ResolveHostCache cache = mCacheGroup.getCache(record.getCacheKey());
			cache.put(record);
		}

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("cache ready");
		}

		//清理db
		ArrayList<HostRecord> expired = new ArrayList<>();
		for (HostRecord record : records) {
			//达到过期阈值
			if (System.currentTimeMillis() >= record.getQueryTime() + record.getTtl() * 1000L + expiredThresholdMillis) {
				expired.add(record);
			}
		}
		mDBHelper.delete(expired);

		if (!mHttpDnsConfig.getRegion().equals(region)) {
			// 防止刚读取完，region变化了
			mCacheGroup.clearAll();
		} else {
			for (final HostRecord record : records) {
				RequestIpType type = RequestIpType.values()[record.getType()];
				if (!record.isExpired() && type == RequestIpType.v4) {
					mIpIPRankingService.probeIpv4(record.getHost(), record.getIps(),
						new IPRankingCallback() {
							@Override
							public void onResult(String host, String[] sortedIps) {
								update(host, RequestIpType.v4, record.getCacheKey(), sortedIps);
							}
						});
				}
			}
		}

	}

	private HostRecord save(String region, String host, RequestIpType type, String extra,
							String cacheKey, String[] ips, int ttl, String serverIp, String noIpCode) {

		if (mCacheTtlChanger != null) {
			ttl = mCacheTtlChanger.changeCacheTtl(host, type, ttl);
		}

		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("save host " + host + " for type " + type.name() + " ttl is " + ttl);
		}

		ResolveHostCache cache = mCacheGroup.getCache(cacheKey);
		return cache.update(region, host, type, extra, cacheKey, ips, ttl, serverIp, noIpCode);
	}

	private void updateInner(String host, RequestIpType type, String cacheKey, String[] ips) {
		ResolveHostCache cache = mCacheGroup.getCache(cacheKey);
		HostRecord record = cache.updateIps(host, type, ips);
		if (mEnableCache || mHostListWhichIpFixed.contains(host)) {
			final ArrayList<HostRecord> records = new ArrayList<>();
			records.add(record);
			try {
				mHttpDnsConfig.getDbWorker().execute(new Runnable() {
					@Override
					public void run() {
						mDBHelper.insertOrUpdate(records);
					}
				});
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 保存预解析结果
	 */
	public void save(String region, ResolveHostResponse resolveHostResponse, String cacheKey) {
		final ArrayList<HostRecord> records = new ArrayList<>();
		for (ResolveHostResponse.HostItem item : resolveHostResponse.getItems()) {
			HostRecord record = save(region, item.getHost(), item.getIpType(), item.getExtra(), cacheKey,
				item.getIps(), item.getTtl(), resolveHostResponse.getServerIp(), item.getNoIpCode());
			if (mEnableCache
				|| mHostListWhichIpFixed.contains(item.getHost())
				|| (item.getIps() == null || item.getIps().length == 0)
			) {
				records.add(record);
			}
		}
		if (records.size() > 0) {
			try {
				mHttpDnsConfig.getDbWorker().execute(new Runnable() {
					@Override
					public void run() {
						mDBHelper.insertOrUpdate(records);
					}
				});
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 更新ip, 一般用于更新ip的顺序
	 */
	public void update(String host, RequestIpType type, String cacheKey, String[] ips) {
		switch (type) {
			case v4:
				updateInner(host, RequestIpType.v4, cacheKey, ips);
				break;
			case v6:
				updateInner(host, RequestIpType.v6, cacheKey, ips);
				break;
			default:
				HttpDnsLog.e("update both is impossible for " + host);
				break;
		}
	}

	/**
	 * 获取之前保存的解析结果
	 */
	public HTTPDNSResultWrapper getIps(String host, RequestIpType type, String cacheKey) {
		ensureReadFromDB();

		ResolveHostCache cache = mCacheGroup.getCache(cacheKey);
		return cache.getResult(host, type);
	}

	/**
	 * 仅清除内容缓存
	 */
	public void clearMemoryCache() {
		mCacheGroup.clearAll();
	}

	/**
	 * 仅清除非主站域名的内存缓存
	 */
	public void clearMemoryCacheForHostWithoutFixedIP() {
		mCacheGroup.clearAll(new ArrayList<String>(getAllHostWithoutFixedIP().keySet()));
	}

	/**
	 * 清除所有已解析结果
	 */
	public void clear() {
		final List<HostRecord> recordsToBeDeleted = mCacheGroup.clearAll();
		if (mEnableCache && recordsToBeDeleted.size() > 0) {
			try {
				mHttpDnsConfig.getDbWorker().execute(new Runnable() {
					@Override
					public void run() {
						mDBHelper.delete(recordsToBeDeleted);
					}
				});
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 清除指定域名的已解析结果
	 */
	public void clear(ArrayList<String> hosts) {
		if (hosts == null || hosts.size() == 0) {
			clear();
			return;
		}
		final List<HostRecord> recordsToBeDeleted = mCacheGroup.clearAll(hosts);
		if (recordsToBeDeleted.size() > 0 && mEnableCache) {
			try {
				mHttpDnsConfig.getDbWorker().execute(new Runnable() {
					@Override
					public void run() {
						mDBHelper.delete(recordsToBeDeleted);
					}
				});
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 获取当前所有已缓存结果的域名
	 */
	public HashMap<String, RequestIpType> getAllHostWithoutFixedIP() {
		HashMap<String, RequestIpType> result = mCacheGroup.getAllHostFromNetworkCaches();
		for (String host : mHostListWhichIpFixed) {
			result.remove(host);
		}
		return result;
	}

	/**
	 * 配置 本地缓存开关，触发缓存读取逻辑
	 */
	public void setCachedIPEnabled(boolean enable, long expiredThresholdMillis) {
		mEnableCache = enable;
		mExpiredThresholdMillis = expiredThresholdMillis;
		try {
			mHttpDnsConfig.getDbWorker().execute(new Runnable() {
				@Override
				public void run() {
					ensureReadFromDB();
				}
			});
		} catch (Throwable ignored) {
		}
	}

	/**
	 * 设置自定义ttl的接口，用于控制缓存的时长
	 */
	public void setCacheTtlChanger(CacheTtlChanger changer) {
		mCacheTtlChanger = changer;
	}

	/**
	 * 设置主站域名，主站域名的缓存策略和其它域名不同
	 * 1. 内存缓存不轻易清除
	 * 2. 默认本地缓存，本地缓存的ttl有效
	 */
	public void setHostListWhichIpFixed(List<String> hostListWhichIpFixed) {
		this.mHostListWhichIpFixed.clear();
		if (hostListWhichIpFixed != null) {
			this.mHostListWhichIpFixed.addAll(hostListWhichIpFixed);
		}
	}

    /**
     * 获取缓存组，供外部访问网络缓存
     *
     * @return 缓存组
     */
    public ResolveHostCacheGroup getCacheGroup() {
        return mCacheGroup;
    }
}
