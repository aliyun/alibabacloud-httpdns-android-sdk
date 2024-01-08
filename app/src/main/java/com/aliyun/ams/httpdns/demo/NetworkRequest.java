package com.aliyun.ams.httpdns.demo;

import com.alibaba.sdk.android.httpdns.RequestIpType;

public interface NetworkRequest {

    /**
     * 设置httpdns的配置
     */
    void updateHttpDnsConfig(boolean async, RequestIpType requestIpType);

    /**
     * get请求
     *
     * @param url
     * @return
     */
    String httpGet(String url) throws Exception;

}
