package com.alibaba.sdk.android.httpdns;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.cache.HostRecord;

import java.util.List;

/**
 * 对解析结果进行封装，用于可观测数据的一些解析数据携带，不对外透出
 */
public class HTTPDNSResultWrapper {
    private final HTTPDNSResult mHTTPDNSResult;
    private String mServerIp;

    public HTTPDNSResultWrapper(String host) {
        mHTTPDNSResult = new HTTPDNSResult(host);
    }

    public HTTPDNSResult getHTTPDNSResult() {
        return mHTTPDNSResult;
    }

    public void update(HostRecord record) {
        mHTTPDNSResult.update(record);
        mServerIp = record.getServerIp();
    }

    public void update(List<HostRecord> records) {
        mHTTPDNSResult.update(records);

        for (HostRecord record : records) {
            if (record.getHost().equals(mHTTPDNSResult.getHost())) {
                if (!TextUtils.isEmpty(record.getServerIp())) {
                    mServerIp = record.getServerIp();
                }
            }
        }
    }

    public boolean isExpired() {
        return mHTTPDNSResult.isExpired();
    }

    public String[] getIps() {
        return mHTTPDNSResult.getIps();
    }

    public String[] getIpv6s() {
        return mHTTPDNSResult.getIpv6s();
    }

    public String getServerIp() {
        return mServerIp;
    }

    @Override
    public String toString() {
        return mHTTPDNSResult.toString();
    }
}
