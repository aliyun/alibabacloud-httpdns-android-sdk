package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 测试用接口
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class DebugApiServer extends BaseDataServer<Void, String> {


    @Override
    public String convert(String body) {
        return body;
    }

    @Override
    public String randomData(Void aVoid) {
        return "hello";
    }

    @Override
    public Void getRequestArg(RecordedRequest recordedRequest) {
        return null;
    }

    @Override
    public boolean isMyBusinessRequest(RecordedRequest request) {
        return isDebugRequest(request);
    }


    /**
     * 服务侧 判断是否是 测试请求
     * @param request
     * @return
     */
    private static boolean isDebugRequest(RecordedRequest request) {
        List<String> pathSegments = request.getRequestUrl().pathSegments();
        return pathSegments.size() == 1 && pathSegments.contains("debug");
    }
}
