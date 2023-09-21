package com.alibaba.sdk.android.httpdns.test.server.base;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * sdk业务服务
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
interface SdkBusinessServer {

    /**
     * 是否是当前业务服务的请求
     *
     * @param request
     * @return
     */
    boolean isMyBusinessRequest(RecordedRequest request);

    /**
     * 处理请求
     *
     * @param request
     * @return
     */
    MockResponse handle(RecordedRequest request);
}
