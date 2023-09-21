package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

/**
 * httpdns服务创建接口
 */
public interface HttpDnsCreator {
    HttpDnsService create(Context context, String accountId, String secretKey);
}
