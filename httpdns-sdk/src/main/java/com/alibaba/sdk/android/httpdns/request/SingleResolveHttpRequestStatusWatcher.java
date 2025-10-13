package com.alibaba.sdk.android.httpdns.request;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.observable.ObservableManager;
import com.alibaba.sdk.android.httpdns.observable.event.QueryHttpDnsApiEvent;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResponse;

public class SingleResolveHttpRequestStatusWatcher implements HttpRequestWatcher.Watcher {
    private final ObservableManager observableManager;

    private QueryHttpDnsApiEvent mQueryHttpDnsApiEvent = null;

    public SingleResolveHttpRequestStatusWatcher(ObservableManager observableManager) {
        this.observableManager = observableManager;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        mQueryHttpDnsApiEvent = new QueryHttpDnsApiEvent();
    }

    @Override
    public void onSuccess(HttpRequestConfig config, Object data) {
        if (mQueryHttpDnsApiEvent != null && observableManager != null) {
            mQueryHttpDnsApiEvent.setTag(config.isRetry(), config.getResolvingIpType(),
                    HttpRequestConfig.HTTP_SCHEMA.equals(config.getSchema()), config.isSignMode());
            mQueryHttpDnsApiEvent.setHostName(config.getResolvingHost());
            mQueryHttpDnsApiEvent.setServerIp(config.getIp());
            mQueryHttpDnsApiEvent.setCostTime((int) (System.currentTimeMillis() - mQueryHttpDnsApiEvent.getTimestamp()));

            int resultIpType = 0x00;
            if (data instanceof ResolveHostResponse) {
                ResolveHostResponse response = (ResolveHostResponse) data;
                String[] ips = null, ipv6s = null;
                for (ResolveHostResponse.HostItem item : response.getItems()) {
                    if (item.getIpType() == RequestIpType.v4) {
                        resultIpType |= 0x01;
                        ips = item.getIps();
                    } else if (item.getIpType() == RequestIpType.v6) {
                        resultIpType |= 0x02;
                        ipv6s = item.getIps();

                    }
                }

                mQueryHttpDnsApiEvent.setHttpDnsIps(ips, ipv6s);
            }
            mQueryHttpDnsApiEvent.setIpType(resultIpType);
            mQueryHttpDnsApiEvent.setStatusCode(resultIpType == 0x00 ? 204 : 200);

            observableManager.addObservableEvent(mQueryHttpDnsApiEvent);
            mQueryHttpDnsApiEvent = null;
        }
    }

    @Override
    public void onFail(HttpRequestConfig config, Throwable throwable) {
        if (mQueryHttpDnsApiEvent != null && observableManager != null) {
            mQueryHttpDnsApiEvent.setTag(config.isRetry(), config.getResolvingIpType(),
                    HttpRequestConfig.HTTP_SCHEMA.equals(config.getSchema()), config.isSignMode());
            mQueryHttpDnsApiEvent.setHostName(config.getResolvingHost());
            mQueryHttpDnsApiEvent.setServerIp(config.getIp());
            mQueryHttpDnsApiEvent.setCostTime((int) (System.currentTimeMillis() - mQueryHttpDnsApiEvent.getTimestamp()));
            mQueryHttpDnsApiEvent.setIpType(0x00);

            if (throwable instanceof HttpException) {
                mQueryHttpDnsApiEvent.setStatusCode(((HttpException) throwable).getCode());
                String errorCode = throwable.getMessage();
                mQueryHttpDnsApiEvent.setErrorCode(TextUtils.isEmpty(errorCode) ? "Unknown" : errorCode);
            } else {
                mQueryHttpDnsApiEvent.setStatusCode(-1);
                mQueryHttpDnsApiEvent.setErrorCode(throwable.getClass().getSimpleName() + ":" + throwable.getMessage());
            }

            observableManager.addObservableEvent(mQueryHttpDnsApiEvent);
            mQueryHttpDnsApiEvent = null;
        }
    }
}
