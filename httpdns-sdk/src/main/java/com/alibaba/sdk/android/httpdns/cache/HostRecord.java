package com.alibaba.sdk.android.httpdns.cache;

import java.util.Arrays;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * ip解析结果记录
 * 注意计算hash 和 equal实现，没有使用fromDB字段
 */
public class HostRecord {
	private long id = -1;
	private String region;
	private String host;
	private String[] ips;
	private int type;
	private int ttl;
	private long queryTime;
	private String extra;
	private String cacheKey;
	private boolean fromDB = false;
	private String serverIp;
	private String noIpCode;

	public static HostRecord create(String region, String host, RequestIpType type, String extra,
									String cacheKey, String[] ips, int ttl, String serverIp, String noIpCode) {
		HostRecord record = new HostRecord();
		record.region = region;
		record.host = host;
		record.type = type.ordinal();
		record.ips = ips;
		record.ttl = ttl;
		record.queryTime = System.currentTimeMillis();
		record.extra = extra;
		record.cacheKey = cacheKey;
		record.serverIp = serverIp;
		record.noIpCode = noIpCode;
		return record;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() > queryTime + ttl * 1000L;
	}

	public void setFromDB(boolean fromDB) {
		this.fromDB = fromDB;
	}

	public boolean isFromDB() {
		return fromDB;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String[] getIps() {
		return ips;
	}

	public void setIps(String[] ips) {
		this.ips = ips;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public long getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(long queryTime) {
		this.queryTime = queryTime;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setServerIp(String ip) {
		serverIp = ip;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setNoIpCode(String noIpCode) {
		this.noIpCode = noIpCode;
	}

	public String getNoIpCode() {
		return noIpCode;
	}

	@Override
	public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
		HostRecord that = (HostRecord)o;
		return id == that.id &&
			type == that.type &&
			ttl == that.ttl &&
			queryTime == that.queryTime &&
			region.equals(that.region) &&
			host.equals(that.host) &&
			Arrays.equals(ips, that.ips) &&
			CommonUtil.equals(extra, that.extra) &&
			CommonUtil.equals(cacheKey, that.cacheKey) &&
			CommonUtil.equals(noIpCode, that.noIpCode);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(
			new Object[] {id, region, host, type, ttl, queryTime, extra, cacheKey, noIpCode});
		result = 31 * result + Arrays.hashCode(ips);
		return result;
	}

	@Override
	public String toString() {
		return "HostRecord{" +
			"id=" + id +
			", region='" + region + '\'' +
			", host='" + host + '\'' +
			", ips=" + Arrays.toString(ips) +
			", type=" + type +
			", ttl=" + ttl +
			", queryTime=" + queryTime +
			", extra='" + extra + '\'' +
			", cacheKey='" + cacheKey + '\'' +
			", fromDB=" + fromDB +
			", noIpCode=" + noIpCode +
			'}';
	}
}
