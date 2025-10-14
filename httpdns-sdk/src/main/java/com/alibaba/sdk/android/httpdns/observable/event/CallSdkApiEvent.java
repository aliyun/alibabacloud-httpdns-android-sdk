package com.alibaba.sdk.android.httpdns.observable.event;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;

public class CallSdkApiEvent extends ObservableEvent implements GroupEvent, LocalDnsEvent {

    public CallSdkApiEvent(long startTime) {
        super();
        mTimestamp = startTime;
        mEventName = 5;
        mCount = 1;
    }

    public void setRequestType(RequestIpType queryType) {
        if (queryType == RequestIpType.v4) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_V4;
        } else if (queryType == RequestIpType.v6) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_V6;
        } else if (queryType == RequestIpType.both) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_BOTH;
        }
    }

    public void setInvokeApi(int api) {
        mTag |= api;
    }

    public void setCacheScene(int scene) {
        mTag |= scene;
    }

    public void setResultStatus(int status) {
        mTag |= status;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    @Override
    public void setErrorCode(String errorCode) {
        //do nothing
    }

    private void incrementCount() {
        ++mCount;
    }

    @Override
    public boolean isSameGroup(ObservableEvent event) {
        if (event == null) {
            return false;
        }

        return TextUtils.equals(getHostName(), event.getHostName())
                && getTag() == event.getTag();
    }

    @Override
    public void groupWith(ObservableEvent event) {
        setTimestamp(event.getTimestamp());
        mServerIp = event.getServerIp();
        int totalCostTime = getCostTime() * getCount();
        incrementCount();
        setCostTime((totalCostTime + event.getCostTime()) / getCount());
        if (!TextUtils.isEmpty(event.getHttpDnsIps())) {
            mHttpDnsIps = event.getHttpDnsIps();
        }
        if (!TextUtils.isEmpty(event.getLocalDnsIps())) {
            mLocalDnsIps = event.getLocalDnsIps();
            mLocalDnsCost = event.getLocalDnsCost();
        }
    }

    @Override
    public int getRequestIpType() {
        return mTag;
    }
}
