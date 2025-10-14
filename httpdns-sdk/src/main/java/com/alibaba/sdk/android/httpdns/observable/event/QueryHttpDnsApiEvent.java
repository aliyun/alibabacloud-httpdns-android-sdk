package com.alibaba.sdk.android.httpdns.observable.event;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;

public class QueryHttpDnsApiEvent extends ObservableEvent implements LocalDnsEvent {
    public QueryHttpDnsApiEvent() {
        super();
        mEventName = 3;
        mCount = 1;
    }

    public void setTag(boolean isRetry, RequestIpType queryType, boolean http, boolean signMode) {
        mTag = isRetry ? ObservableConstants.REQUEST_RETRY : ObservableConstants.REQUEST_NOT_RETRY;
        if (queryType == RequestIpType.v4) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_V4;
        } else if (queryType == RequestIpType.v6) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_V6;
        } else if (queryType == RequestIpType.both) {
            mTag |= ObservableConstants.REQUEST_IP_TYPE_BOTH;
        }

        if (!http) {
            mTag |= ObservableConstants.HTTPS_REQUEST;
        }

        if (signMode) {
            mTag |= ObservableConstants.SIGN_MODE_REQUEST;
        }
    }

    @Override
    public int getRequestIpType() {
        return mTag;
    }
}
