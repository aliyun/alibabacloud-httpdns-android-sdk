package com.alibaba.sdk.android.httpdns.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.sdk.android.httpdns.cache.HostRecord;

/**
 * 获取的缓存
 */
public class ResolveHostCacheGroup {

	/**
	 * 默认缓存
	 */
	private final ResolveHostCache mDefaultCache = new ResolveHostCache();
	/**
	 * 使用sdns时，不同CacheKey对应的缓存
	 */
	private final HashMap<String, ResolveHostCache> mSdnsCaches = new HashMap<>();
	private final Object lock = new Object();

	public ResolveHostCache getCache(String cacheKey) {
		if (cacheKey == null || cacheKey.isEmpty()) {
			return mDefaultCache;
		}
		ResolveHostCache cache = mSdnsCaches.get(cacheKey);
		if (cache == null) {
			synchronized (lock) {
				cache = mSdnsCaches.get(cacheKey);
				if (cache == null) {
					cache = new ResolveHostCache();
					mSdnsCaches.put(cacheKey, cache);
				}
			}
		}
		return cache;
	}

	public List<HostRecord> clearAll() {
        ArrayList<HostRecord> records = new ArrayList<>(mDefaultCache.clear());
		if (mSdnsCaches.size() > 0) {
			synchronized (lock) {
				for (ResolveHostCache cache : mSdnsCaches.values()) {
					records.addAll(cache.clear());
				}
			}
		}
		return records;
	}

	public List<HostRecord> clearAll(List<String> hosts) {
        ArrayList<HostRecord> records = new ArrayList<>(mDefaultCache.clear(hosts));
		if (mSdnsCaches.size() > 0) {
			synchronized (lock) {
				for (ResolveHostCache cache : mSdnsCaches.values()) {
					records.addAll(cache.clear(hosts));
				}
			}
		}
		return records;
	}
}
