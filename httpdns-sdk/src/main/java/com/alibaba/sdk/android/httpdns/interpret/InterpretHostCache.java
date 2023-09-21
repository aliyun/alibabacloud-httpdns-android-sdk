package com.alibaba.sdk.android.httpdns.interpret;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;

public class InterpretHostCache {

	/**
	 * v4的解析记录
	 */
	private final ConcurrentHashMap<String, HostRecord> mV4Records = new ConcurrentHashMap<>();
	/**
	 * v6的解析记录
	 */
	private final ConcurrentHashMap<String, HostRecord> mV6Records = new ConcurrentHashMap<>();
	/**
	 * v4的返回结果
	 */
	private final ConcurrentHashMap<String, HTTPDNSResult> mV4HttpDnsResults = new ConcurrentHashMap<>();
	/**
	 * v6的返回结果
	 */
	private final ConcurrentHashMap<String, HTTPDNSResult> mV6HttpDnsResults = new ConcurrentHashMap<>();
	/**
	 * 同时解析4 6的结果
	 */
	private final ConcurrentHashMap<String, HTTPDNSResult> mBothHttpDnsResults =
        new ConcurrentHashMap<>();

	public HTTPDNSResult getResult(String host, RequestIpType type) {
		HTTPDNSResult result = obtainHttpResult(host, type);
		result = buildHttpResult(host, type, result);
		cacheResult(host, type, result);
		return result;
	}

	private HTTPDNSResult obtainHttpResult(String host, RequestIpType type) {
		HTTPDNSResult result = null;
		switch (type) {
			case v6:
				result = mV6HttpDnsResults.get(host);
				break;
			case v4:
				result = mV4HttpDnsResults.get(host);
				break;
			case both:
				result = mBothHttpDnsResults.get(host);
				break;
		}
		return result;
	}

	private void cacheResult(String host, RequestIpType type, HTTPDNSResult result) {
		if (result != null) {
			switch (type) {
				case v6:
					mV6HttpDnsResults.put(host, result);
					break;
				case v4:
					mV4HttpDnsResults.put(host, result);
					break;
				case both:
					mBothHttpDnsResults.put(host, result);
					break;
			}
		}
	}

	private HTTPDNSResult buildHttpResult(String host, RequestIpType type, HTTPDNSResult result) {
		HostRecord record;
		switch (type) {
			case v6:
				record = mV6Records.get(host);
				if (record != null) {
					if (result == null) {
						result = new HTTPDNSResult(host);
					}
					result.update(record);
				}
				break;
			case v4:
				record = mV4Records.get(host);
				if (record != null) {
					if (result == null) {
						result = new HTTPDNSResult(host);
					}
					result.update(record);
				}
				break;
			default:
				record = mV4Records.get(host);
				HostRecord recordv6 = mV6Records.get(host);
				if (record == null || recordv6 == null) {
					return result;
				}
				if (result == null) {
					result = new HTTPDNSResult(host);
				}
				ArrayList<HostRecord> records = new ArrayList<>();
				records.add(record);
				records.add(recordv6);
				result.update(records);
				break;
		}
		return result;
	}

	public HostRecord update(String region, String host, RequestIpType type, String extra,
							 String cacheKey, String[] ips, int ttl) {
		HostRecord record = null;
		switch (type) {
			case v4:
				record = mV4Records.get(host);
				if (record == null) {
					record = HostRecord.create(region, host, type, extra, cacheKey, ips, ttl);
					mV4Records.put(host, record);
				} else {
					record.setRegion(region);
					record.setQueryTime(System.currentTimeMillis());
					record.setIps(ips);
					record.setTtl(ttl);
					record.setExtra(extra);
					record.setFromDB(false);
				}
				break;
			case v6:
				record = mV6Records.get(host);
				if (record == null) {
					record = HostRecord.create(region, host, type, extra, cacheKey, ips, ttl);
					mV6Records.put(host, record);
				} else {
					record.setRegion(region);
					record.setQueryTime(System.currentTimeMillis());
					record.setIps(ips);
					record.setTtl(ttl);
					record.setExtra(extra);
					record.setFromDB(false);
				}
				break;
			default:
				throw new IllegalStateException("type should be v4 or b6");
		}
		return record;
	}

	public void put(HostRecord record) {
		if (record.getType() == RequestIpType.v4.ordinal()) {
			mV4Records.put(record.getHost(), record);
		} else if (record.getType() == RequestIpType.v6.ordinal()) {
			mV6Records.put(record.getHost(), record);
		}
	}

	public HostRecord updateIps(String host, RequestIpType type, String[] ips) {
		HostRecord record = null;
		switch (type) {
			case v4:
				record = mV4Records.get(host);
				if (record == null) {
					return null;
				}
				record.setIps(ips);
				break;
			case v6:
				record = mV6Records.get(host);
				if (record == null) {
					return null;
				}
				record.setIps(ips);
				break;
			default:
				throw new IllegalStateException("type should be v4 or b6");
		}
		return record;
	}

	public List<HostRecord> clear() {
		ArrayList<HostRecord> list = new ArrayList<>();
		list.addAll(mV4Records.values());
		list.addAll(mV6Records.values());
		mV4Records.clear();
		mV6Records.clear();
		mV4HttpDnsResults.clear();
		mV6HttpDnsResults.clear();
		mBothHttpDnsResults.clear();
		return list;
	}

	public List<HostRecord> clear(List<String> hosts) {
		ArrayList<HostRecord> records = new ArrayList<>();
		for (String host : hosts) {
			HostRecord tmp = mV4Records.remove(host);
			if (tmp != null) {
				records.add(tmp);
			}
		}
		for (String host : hosts) {
			HostRecord tmp = mV6Records.remove(host);
			if (tmp != null) {
				records.add(tmp);
			}
		}
		for (String host : hosts) {
			mV4HttpDnsResults.remove(host);
			mV6HttpDnsResults.remove(host);
			mBothHttpDnsResults.remove(host);
		}
		return records;
	}

	public HashMap<String, RequestIpType> getAllHostNotEmptyResult() {
		HashMap<String, RequestIpType> all = new HashMap<>();
		for (HostRecord record : mV4Records.values()) {
			if (record.getIps() != null && record.getIps().length > 0) {
				all.put(record.getHost(), RequestIpType.v4);
			}
		}
		for (HostRecord record : mV6Records.values()) {
			if (record.getIps() != null && record.getIps().length > 0) {
				RequestIpType type = all.get(record.getHost());
				if (type == null) {
					all.put(record.getHost(), RequestIpType.v6);
				} else {
					all.put(record.getHost(), RequestIpType.both);
				}
			}
		}
		return all;
	}
}
