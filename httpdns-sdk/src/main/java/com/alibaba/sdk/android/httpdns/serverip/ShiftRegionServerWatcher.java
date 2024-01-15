package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;

/**
 * 请求失败时，切换服务IP，服务IP都切换过，使用初始IP
 */
public class ShiftRegionServerWatcher implements HttpRequestWatcher.Watcher {

    private final RegionServer[] mServers;
    private int currentIndex = 0;

    public ShiftRegionServerWatcher(RegionServer[] servers) {
        this.mServers = servers;
    }

    @Override
    public void onStart(HttpRequestConfig config) {

    }

    @Override
    public void onSuccess(HttpRequestConfig requestConfig, Object data) {
    }

    @Override
    public void onFail(HttpRequestConfig requestConfig, Throwable throwable) {
        // 切换和更新请求的服务IP
        currentIndex++;

        if (currentIndex < mServers.length) {
            // 更新请求用的IP
            requestConfig.setIp(mServers[currentIndex].getServerIp());
            requestConfig.setPort(mServers[currentIndex].getPort(requestConfig.getSchema()));
        }
    }
}
