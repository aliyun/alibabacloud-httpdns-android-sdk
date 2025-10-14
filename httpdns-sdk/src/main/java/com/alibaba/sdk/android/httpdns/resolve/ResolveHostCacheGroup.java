package com.alibaba.sdk.android.httpdns.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;

/**
 * 缓存组管理器，支持网络隔离缓存
 */
public class ResolveHostCacheGroup {

	/**
	 * 所有缓存统一管理，包括网络隔离缓存和SDNS缓存
	 */
	private final HashMap<String, ResolveHostCache> mCaches = new HashMap<>();
	private final Object lock = new Object();
	private static final String NETWORK_KEY_PREFIX = "emasInner_";

	public ResolveHostCache getCache(String cacheKey) {
		if (cacheKey == null || cacheKey.isEmpty()) {
			// 普通解析使用当前网络标识
			cacheKey = NetworkStateManager.getInstance().getCurrentNetworkKey();
		}

		// 统一的缓存获取逻辑
        ResolveHostCache cache = mCaches.get(cacheKey);
		if (cache == null) {
			synchronized (lock) {
				cache = mCaches.get(cacheKey);
				if (cache == null) {
					cache = new ResolveHostCache();
					mCaches.put(cacheKey, cache);
				}
			}
		}
		return cache;
	}

	/**
	 * 获取所有网络缓存中的域名（排除SDNS缓存）
	 */
	public HashMap<String, RequestIpType> getAllHostFromNetworkCaches() {
		HashMap<String, RequestIpType> allHosts = new HashMap<>();

		synchronized (lock) {
			for (Map.Entry<String, ResolveHostCache> entry : mCaches.entrySet()) {
				String cacheKey = entry.getKey();

				if (!cacheKey.startsWith(NETWORK_KEY_PREFIX)) {
					continue;
				}

				ResolveHostCache cache = entry.getValue();
				HashMap<String, RequestIpType> hosts = cache.getAllHostNotEmptyResult();

				for (Map.Entry<String, RequestIpType> hostEntry : hosts.entrySet()) {
					String host = hostEntry.getKey();
					RequestIpType type = hostEntry.getValue();

					if (!allHosts.containsKey(host)) {
						allHosts.put(host, type);
						continue;
					}

					RequestIpType mergedType = mergeIpTypes(allHosts.get(host), type);
					allHosts.put(host, mergedType);
				}
			}
		}
		return allHosts;
	}

	private RequestIpType mergeIpTypes(RequestIpType type1, RequestIpType type2) {
		if (type1 == RequestIpType.both || type2 == RequestIpType.both) {
			return RequestIpType.both;
		}

		if ((type1 == RequestIpType.v4 && type2 == RequestIpType.v6) ||
				(type1 == RequestIpType.v6 && type2 == RequestIpType.v4)) {
			return RequestIpType.both;
		}

		return type1;
	}

	public List<HostRecord> clearAll() {
        ArrayList<HostRecord> records = new ArrayList<>();
		if (mCaches.size() > 0) {
			synchronized (lock) {
				for (ResolveHostCache cache : mCaches.values()) {
					records.addAll(cache.clear());
				}
			}
		}
		return records;
	}

	public List<HostRecord> clearAll(List<String> hosts) {
        ArrayList<HostRecord> records = new ArrayList<>();
		if (mCaches.size() > 0) {
			synchronized (lock) {
				for (ResolveHostCache cache : mCaches.values()) {
					records.addAll(cache.clear(hosts));
				}
			}
		}
		return records;
	}
}
