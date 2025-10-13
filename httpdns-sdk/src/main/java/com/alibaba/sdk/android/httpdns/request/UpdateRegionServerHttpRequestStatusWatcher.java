package com.alibaba.sdk.android.httpdns.request;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.observable.ObservableManager;
import com.alibaba.sdk.android.httpdns.observable.event.UpdateRegionServerIpsEvent;

public class UpdateRegionServerHttpRequestStatusWatcher implements HttpRequestWatcher.Watcher {
    private final ObservableManager observableManager;
    private int mScenes;
    private UpdateRegionServerIpsEvent mUpdateRegionServerIpsEvent;

    public UpdateRegionServerHttpRequestStatusWatcher(int scenes, ObservableManager observableManager) {
        this.observableManager = observableManager;
        mScenes = scenes;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        mUpdateRegionServerIpsEvent = new UpdateRegionServerIpsEvent();
        mUpdateRegionServerIpsEvent.setTag(mScenes);
    }

    @Override
    public void onSuccess(HttpRequestConfig config, Object data) {
        if (mUpdateRegionServerIpsEvent != null && observableManager != null) {
            mUpdateRegionServerIpsEvent.setCostTime((int) (System.currentTimeMillis() - mUpdateRegionServerIpsEvent.getTimestamp()));
            mUpdateRegionServerIpsEvent.setStatusCode(200);

            observableManager.addObservableEvent(mUpdateRegionServerIpsEvent);
            mUpdateRegionServerIpsEvent = null;
        }
    }

    @Override
    public void onFail(HttpRequestConfig config, Throwable throwable) {
        if (mUpdateRegionServerIpsEvent != null && observableManager != null) {
            mUpdateRegionServerIpsEvent.setCostTime((int) (System.currentTimeMillis() - mUpdateRegionServerIpsEvent.getTimestamp()));
            if (throwable instanceof HttpException) {
                mUpdateRegionServerIpsEvent.setStatusCode(((HttpException) throwable).getCode());
                String errorCode = throwable.getMessage();
                mUpdateRegionServerIpsEvent.setErrorCode(TextUtils.isEmpty(errorCode) ? "Unknown" : errorCode);
            } else {
                mUpdateRegionServerIpsEvent.setStatusCode(-1);
                mUpdateRegionServerIpsEvent.setErrorCode(throwable.getClass().getSimpleName() + ":" + throwable.getMessage());
            }

            observableManager.addObservableEvent(mUpdateRegionServerIpsEvent);
            mUpdateRegionServerIpsEvent = null;
        }
    }
}
