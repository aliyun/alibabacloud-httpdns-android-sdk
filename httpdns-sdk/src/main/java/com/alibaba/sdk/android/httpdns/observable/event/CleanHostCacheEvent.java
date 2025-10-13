package com.alibaba.sdk.android.httpdns.observable.event;

public class CleanHostCacheEvent extends ObservableEvent {

    public CleanHostCacheEvent() {
        super();
        mEventName = 6;
        mCount = 1;
    }

    public void setTag(int cleanType) {
        mTag = cleanType;
    }

    @Override
    public void setHostName(String hostName) {
        //do nothing
    }

    @Override
    public void setServerIp(String serverIp) {
        //do nothing
    }

    @Override
    public void setIpType(int type) {
        //do nothing
    }

    @Override
    public void setErrorCode(String errorCode) {
        //do nothing
    }

    @Override
    public void setHttpDnsIps(String[] ipv4s, String[] ipv6s) {
        //do nothing
    }

    @Override
    public void setLocalDnsIps(String localDnsIps) {
        //do nothing
    }
}
