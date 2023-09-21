package com.aliyun.ams.httpdns.demo;

import com.alibaba.sdk.android.httpdns.RequestIpType;

/**
 * @author zonglin.nzl
 * @date 8/31/22
 */
public interface NetworkRequest {

    /**
     * 设置httpdns的配置
     *
     * @param async
     * @param requestIpType
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
