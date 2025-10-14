package com.alibaba.sdk.android.httpdns.observable.event;

public class BatchQueryHttpDnsApiEvent extends QueryHttpDnsApiEvent {

    public BatchQueryHttpDnsApiEvent() {
        super();
        mEventName = 2;
    }

    @Override
    public void setHostName(String hostName) {
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
