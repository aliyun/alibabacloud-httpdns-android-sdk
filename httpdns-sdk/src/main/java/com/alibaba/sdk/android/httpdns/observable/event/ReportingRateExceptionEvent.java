package com.alibaba.sdk.android.httpdns.observable.event;

public class ReportingRateExceptionEvent extends ObservableEvent {

    public ReportingRateExceptionEvent() {
        super();
        mEventName = 8;
        mCount = 1;
    }

    public void setTag(int eventName) {
        mTag = eventName;
    }

    @Override
    public void setHostName(String hostName) {
        //do nothing
    }

    @Override
    public void setCostTime(int time) {
        mCostTime = 0;
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
