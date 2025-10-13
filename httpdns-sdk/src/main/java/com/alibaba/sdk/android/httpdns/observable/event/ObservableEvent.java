package com.alibaba.sdk.android.httpdns.observable.event;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsServiceImpl;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;
import com.alibaba.sdk.android.httpdns.utils.NetworkUtil;

public abstract class ObservableEvent {
    private static final String DIVIDER = "|";
    protected int mEventName;
    protected int mTag;
    protected String mHostName;
    protected long mTimestamp;
    protected String mServerIp;
    protected int mCostTime;
    private String mIpType;
    protected int mStatusCode;
    protected String mErrorCode;
    protected int mCount;
    private String mNetworkType;
    protected String mHttpDnsIps;
    protected String mLocalDnsIps;
    protected long mLocalDnsCost;

    protected ObservableEvent() {
        mTimestamp = System.currentTimeMillis();
        mNetworkType = String.valueOf(NetworkUtil.getNetworkType(HttpDnsServiceImpl.sContext));
    }

    public int getEventName() {
        return mEventName;
    }

    public int getTag() {
        return mTag;
    }

    public void setHostName(String hostName) {
        mHostName = hostName;
    }

    public String getHostName() {
        return mHostName;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setServerIp(String serverIp) {
        mServerIp = serverIp;
    }

    public String getServerIp() {
        return mServerIp;
    }

    public void setCostTime(int time) {
        mCostTime = time;
    }

    public int getCostTime() {
        return mCostTime;
    }

    public void setIpType(int type) {
        mIpType = String.valueOf(type);
    }

    public int getIpType() {
        return Integer.parseInt(mIpType);
    }

    public void setStatusCode(int statusCode) {
        mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public void setErrorCode(String errorCode) {
        if (TextUtils.isEmpty(errorCode)) {
            return;
        }

        if (errorCode.length() > 128) {
            mErrorCode = errorCode.substring(0, 128);
        } else {
            mErrorCode = errorCode;
        }
    }

    public String getErrorCode() {
        return mErrorCode;
    }

    public int getCount() {
        return mCount;
    }

    public void setHttpDnsIps(String[] ipv4s, String[] ipv6s) {
        int ipType = 0;
        if (ipv4s != null && ipv4s.length > 0) {
            ipType |= ObservableConstants.REQUEST_IP_TYPE_V4;
        }

        if (ipv6s != null && ipv6s.length > 0) {
            ipType |= ObservableConstants.REQUEST_IP_TYPE_V6;
        }

        setIpType(ipType);

        StringBuilder sb = new StringBuilder();
        if (ipv4s != null) {
            for (int i = 0; i != ipv4s.length; ++i) {
                sb.append(ipv4s[i]);
                if (i != ipv4s.length - 1) {
                    sb.append(",");
                }
            }
        }

        if (ipv6s != null) {
            if (ipv6s.length != 0 && sb.length() > 0) {
                sb.append(";");
            }
            for (int i = 0; i != ipv6s.length; ++i) {
                sb.append(ipv6s[i]);
                if (i != ipv6s.length - 1) {
                    sb.append(",");
                }
            }
        }

        mHttpDnsIps = sb.toString();
    }

    public String getHttpDnsIps() {
        return mHttpDnsIps;
    }

    public void setLocalDnsIps(String localDnsIps) {
        mLocalDnsIps = localDnsIps;
    }

    public String getLocalDnsIps() {
        return mLocalDnsIps;
    }

    public void setLocalDnsCost(long localDnsCost) {
        mLocalDnsCost = localDnsCost;
    }

    public long getLocalDnsCost() {
        return mLocalDnsCost;
    }

    public String toString() {
        return mEventName +
                DIVIDER + mTag +
                DIVIDER + (TextUtils.isEmpty(mHostName) ? "" : mHostName) +
                DIVIDER + mTimestamp +
                DIVIDER + (TextUtils.isEmpty(mServerIp) ? "" : mServerIp) +
                DIVIDER + mCostTime +
                DIVIDER + (TextUtils.isEmpty(mNetworkType) ? "" : mNetworkType) + //预留的网络类型
                DIVIDER + ((TextUtils.isEmpty(mIpType)) ? "" : mIpType) +
                DIVIDER + mStatusCode +
                DIVIDER + (TextUtils.isEmpty(mErrorCode) ? "" : mErrorCode) +
                DIVIDER + mCount +
                DIVIDER + (TextUtils.isEmpty(mHttpDnsIps) ? "" : mHttpDnsIps) +
                DIVIDER + (TextUtils.isEmpty(mLocalDnsIps) ? "" : mLocalDnsIps) +
                DIVIDER + mLocalDnsCost;
    }
}
