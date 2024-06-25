package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sdns的解析结果
 */
public class HTTPDNSResult {
    String host;
    String[] ips;
    String[] ipv6s;
    Map<String, String> extra;
    long queryTime;
    int ttl;
    boolean fromDB;

    public HTTPDNSResult(String host) {
        this.host = host;
        this.ips = Constants.NO_IPS;
        this.ipv6s = Constants.NO_IPS;
        this.extra = Constants.NO_EXTRA;
        queryTime = 0;
        ttl = 0;
        fromDB = false;
    }

    public HTTPDNSResult(String host, String[] ips, String[] ipv6s, Map<String, String> extra, boolean expired, boolean isFromDB) {
        this.host = host;
        this.ips = ips;
        this.ipv6s = ipv6s;
        this.extra = extra;
        this.queryTime = System.currentTimeMillis();
        this.ttl = 60;
        this.fromDB = isFromDB;
    }

    public static HTTPDNSResult empty(String host) {
        return new HTTPDNSResult(host, new String[0], new String[0], new HashMap<String, String>(), false, false);
    }

    public void update(HostRecord record) {
        if (record.getHost().equals(host)) {
            if (record.getType() == RequestIpType.v4.ordinal()) {
                ips = record.getIps();
            } else if (record.getType() == RequestIpType.v6.ordinal()) {
                ipv6s = record.getIps();
            }
            extra = CommonUtil.toMap(record.getExtra());
            queryTime = record.getQueryTime();
            ttl = record.getTtl();
            fromDB = record.isFromDB();
        }
    }

    public void update(List<HostRecord> records) {
        String extra = null;
        long queryTime = System.currentTimeMillis();
        int ttl = Integer.MAX_VALUE;
        boolean fromDB = false;

        for (HostRecord record : records) {
            if (record.getHost().equals(host)) {
                if (record.getType() == RequestIpType.v4.ordinal()) {
                    ips = record.getIps();
                } else if (record.getType() == RequestIpType.v6.ordinal()) {
                    ipv6s = record.getIps();
                }
                if (record.getExtra() != null && !record.getExtra().isEmpty()) {
                    extra = record.getExtra();
                }
                if (queryTime > record.getQueryTime()) {
                    queryTime = record.getQueryTime();
                }
                if (ttl > record.getTtl()) {
                    ttl = record.getTtl();
                }
                fromDB |= record.isFromDB();
            }
        }
        this.extra = CommonUtil.toMap(extra);
        this.queryTime = queryTime;
        this.ttl = ttl;
        this.fromDB = fromDB;
    }

    @Override
    public String toString() {
        String sb = "host:"
            + host
            + ", ips:"
            + Arrays.toString(ips)
            + ", ipv6s:"
            + Arrays.toString(ipv6s)
            + ", extras:"
            + extra
            + ", expired:"
            + isExpired()
            + ", fromDB:"
            + fromDB;
        return sb;
    }

    public String getHost() {
        return host;
    }

    public String[] getIps() {
        return ips;
    }

    public String[] getIpv6s() {
        return ipv6s;
    }

    public Map<String, String> getExtras() {
        return extra;
    }

    public boolean isExpired() {
        return Math.abs(System.currentTimeMillis() - queryTime) > ttl * 1000L;
    }

    public boolean isFromDB() {
        return fromDB;
    }
}
