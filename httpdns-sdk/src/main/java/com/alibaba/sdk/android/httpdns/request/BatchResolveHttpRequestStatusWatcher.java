package com.alibaba.sdk.android.httpdns.request;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.observable.ObservableConstants;
import com.alibaba.sdk.android.httpdns.observable.ObservableManager;
import com.alibaba.sdk.android.httpdns.observable.event.BatchQueryHttpDnsApiEvent;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResponse;

public class BatchResolveHttpRequestStatusWatcher implements HttpRequestWatcher.Watcher {

    ObservableManager observableManager;

    private BatchQueryHttpDnsApiEvent mBatchQueryHttpDnsApiEvent;

    public BatchResolveHttpRequestStatusWatcher(ObservableManager observableManager) {
        this.observableManager = observableManager;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        mBatchQueryHttpDnsApiEvent = new BatchQueryHttpDnsApiEvent();
    }

    @Override
    public void onSuccess(HttpRequestConfig config, Object data) {
        if (mBatchQueryHttpDnsApiEvent != null && observableManager != null) {
            mBatchQueryHttpDnsApiEvent.setTag(config.isRetry(), config.getResolvingIpType(),
                    HttpRequestConfig.HTTP_SCHEMA.equals(config.getSchema()), config.isSignMode());
            mBatchQueryHttpDnsApiEvent.setServerIp(config.getIp());
            mBatchQueryHttpDnsApiEvent.setCostTime((int) (System.currentTimeMillis() - mBatchQueryHttpDnsApiEvent.getTimestamp()));

            int resultIpType = 0x00;
            if (data instanceof ResolveHostResponse) {
                for (ResolveHostResponse.HostItem item : ((ResolveHostResponse) data).getItems()) {
                    if (item.getIpType() == RequestIpType.v4) {
                        resultIpType |= ObservableConstants.REQUEST_IP_TYPE_V4;
                    } else if (item.getIpType() == RequestIpType.v6) {
                        resultIpType |= ObservableConstants.REQUEST_IP_TYPE_V6;
                    }
                }
            }
            mBatchQueryHttpDnsApiEvent.setIpType(resultIpType);
            mBatchQueryHttpDnsApiEvent.setStatusCode(resultIpType == 0x00 ? 204 : 200);

            observableManager.addObservableEvent(mBatchQueryHttpDnsApiEvent);
            mBatchQueryHttpDnsApiEvent = null;
        }
    }

    @Override
    public void onFail(HttpRequestConfig config, Throwable throwable) {
        if (mBatchQueryHttpDnsApiEvent != null && observableManager != null) {
            mBatchQueryHttpDnsApiEvent.setTag(config.isRetry(), config.getResolvingIpType(),
                    HttpRequestConfig.HTTP_SCHEMA.equals(config.getSchema()), config.isSignMode());
            mBatchQueryHttpDnsApiEvent.setServerIp(config.getIp());
            mBatchQueryHttpDnsApiEvent.setCostTime((int) (System.currentTimeMillis() - mBatchQueryHttpDnsApiEvent.getTimestamp()));
            mBatchQueryHttpDnsApiEvent.setIpType(0x00);
            if (throwable instanceof HttpException) {
                mBatchQueryHttpDnsApiEvent.setStatusCode(((HttpException) throwable).getCode());
                String errorCode = throwable.getMessage();
                mBatchQueryHttpDnsApiEvent.setErrorCode(TextUtils.isEmpty(errorCode) ? "Unknown" : errorCode);
            } else {
                mBatchQueryHttpDnsApiEvent.setStatusCode(-1);
                mBatchQueryHttpDnsApiEvent.setErrorCode(throwable.getClass().getSimpleName() + ":" + throwable.getMessage());
            }

            observableManager.addObservableEvent(mBatchQueryHttpDnsApiEvent);
            mBatchQueryHttpDnsApiEvent = null;
        }
    }
}
