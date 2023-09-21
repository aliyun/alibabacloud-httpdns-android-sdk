package com.alibaba.sdk.android.httpdns.test.server.base;

/**
 * 服务接受到请求的监听
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public interface RequestListener {
    void onRequest(int type, Object arg, BaseDataServer server);
}
