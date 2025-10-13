package com.alibaba.sdk.android.httpdns.observable.event;

import com.alibaba.sdk.android.httpdns.utils.Constants;

public class UpdateRegionServerIpsEvent extends ObservableEvent {

    public UpdateRegionServerIpsEvent() {
        super();
        mEventName = 7;
        mCount = 1;
    }

    public void setTag(int scenes) {
        if (scenes == Constants.UPDATE_REGION_SERVER_SCENES_INIT) {
            mTag = 3;
        } else if (scenes == Constants.UPDATE_REGION_SERVER_SCENES_REGION_CHANGE) {
            mTag = 2;
        } else if (scenes == Constants.UPDATE_REGION_SERVER_SCENES_SERVER_UNAVAILABLE) {
            mTag = 1;
        }
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
