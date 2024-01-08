package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ErrorImpl implements HttpDnsService, SyncService {
    @Override
    public void setLogEnabled(boolean shouldPrintLog) {

    }

    @Override
    public void setPreResolveHosts(ArrayList<String> hostList) {

    }

    @Override
    public void setPreResolveHosts(ArrayList<String> hostList, RequestIpType requestIpType) {

    }

    @Override
    public String getIpByHostAsync(String host) {
        HttpDnsLog.w("init error");
        return null;
    }

    @Override
    public String getIPv4ForHostAsync(String host) {
        return null;
    }

    @Override
    public String[] getIpsByHostAsync(String host) {
        HttpDnsLog.w("init error");
        return new String[0];
    }

    @Override
    public String[] getIPv4ListForHostAsync(String host) {
        return new String[0];
    }

    @Override
    public String[] getIPv6sByHostAsync(String host) {
        HttpDnsLog.w("init error");
        return new String[0];
    }

    @Override
    public String[] getIPv6ListForHostASync(String host) {
        return new String[0];
    }

    @Override
    public HTTPDNSResult getAllByHostAsync(String host) {
        HttpDnsLog.w("init error");
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getHttpDnsResultForHostAsync(String host) {
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type) {
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type) {
        return Constants.EMPTY;
    }

    @Override
    public void setExpiredIPEnabled(boolean enable) {

    }

    @Override
    public void setCachedIPEnabled(boolean enable) {

    }

    @Override
    public void setCachedIPEnabled(boolean enable, boolean autoCleanCacheAfterLoad) {

    }

    @Override
    public void setAuthCurrentTime(long time) {

    }

    @Override
    public void setDegradationFilter(DegradationFilter filter) {

    }

    @Override
    public void setPreResolveAfterNetworkChanged(boolean enable) {

    }

    @Override
    public void setTimeoutInterval(int timeoutInterval) {

    }

    @Override
    public void setHTTPSRequestEnabled(boolean enabled) {

    }

    @Override
    public void setIPProbeList(List<IPRankingBean> ipProbeList) {

    }

    @Override
    public void setIPRankingList(List<IPRankingBean> ipRankingList) {

    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public void setLogger(ILogger logger) {

    }

    @Override
    public HTTPDNSResult getIpsByHostAsync(String host, Map<String, String> params,
                                           String cacheKey) {
        HttpDnsLog.w("init error");
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getHttpDnsResultForHostAsync(String host, Map<String, String> params,
                                                      String cacheKey) {
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type,
                                           Map<String, String> params, String cacheKey) {
        HttpDnsLog.w("init error");
        return Constants.EMPTY;
    }

    @Override
    public HTTPDNSResult getHttpDnsResultForHostAsync(String host, RequestIpType type,
                                                      Map<String, String> params, String cacheKey) {
        return Constants.EMPTY;
    }

    @Override
    public void setSdnsGlobalParams(Map<String, String> params) {

    }

    @Override
    public void clearSdnsGlobalParams() {

    }

    @Override
    public void setRegion(String region) {

    }

    @Override
    public String getIPv6ByHostAsync(String host) {
        HttpDnsLog.w("init error");
        return null;
    }

    @Override
    public String getIPv6ForHostAsync(String host) {
        return null;
    }

    @Override
    public HTTPDNSResult getByHost(String host, RequestIpType type) {
        HttpDnsLog.w("init error");
        return Constants.EMPTY;
    }

    @Override
    public void cleanHostCache(ArrayList<String> hosts) {
    }

    @Override
    public void enableCrashDefend(boolean enabled) {

    }

    @Override
    public HTTPDNSResult getHttpDnsResultForHostSync(String host, RequestIpType type) {
        return Constants.EMPTY;
    }
}
