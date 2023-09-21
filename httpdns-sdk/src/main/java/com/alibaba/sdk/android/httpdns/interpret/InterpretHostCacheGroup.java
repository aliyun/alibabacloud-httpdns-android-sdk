package com.alibaba.sdk.android.httpdns.interpret;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.alibaba.sdk.android.httpdns.cache.HostRecord;

/**
 * 获取的缓存
 */
public class InterpretHostCacheGroup {

	/**
	 * 默认缓存
	 */
	private final InterpretHostCache mDefaultCache = new InterpretHostCache();
	/**
	 * 使用sdns时，不同CacheKey对应的缓存
	 */
	private final HashMap<String, InterpretHostCache> sdnsCaches = new HashMap<>();
	private final Object lock = new Object();

	public InterpretHostCache getCache(String cacheKey) {
		if (cacheKey == null || cacheKey.isEmpty()) {
			return mDefaultCache;
		}
		InterpretHostCache cache = sdnsCaches.get(cacheKey);
		if (cache == null) {
			synchronized (lock) {
				cache = sdnsCaches.get(cacheKey);
				if (cache == null) {
					cache = new InterpretHostCache();
					sdnsCaches.put(cacheKey, cache);
				}
			}
		}
		return cache;
	}

	public List<HostRecord> clearAll() {
        ArrayList<HostRecord> records = new ArrayList<>(mDefaultCache.clear());
		if (sdnsCaches.size() > 0) {
			synchronized (lock) {
				for (InterpretHostCache cache : sdnsCaches.values()) {
					records.addAll(cache.clear());
				}
			}
		}
		return records;
	}

	public List<HostRecord> clearAll(List<String> hosts) {
        ArrayList<HostRecord> records = new ArrayList<>(mDefaultCache.clear(hosts));
		if (sdnsCaches.size() > 0) {
			synchronized (lock) {
				for (InterpretHostCache cache : sdnsCaches.values()) {
					records.addAll(cache.clear(hosts));
				}
			}
		}
		return records;
	}
}
