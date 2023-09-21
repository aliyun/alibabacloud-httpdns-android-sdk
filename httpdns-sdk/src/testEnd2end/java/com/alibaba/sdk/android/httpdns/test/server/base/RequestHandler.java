package com.alibaba.sdk.android.httpdns.test.server.base;

import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;

import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务的请求处理逻辑
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class RequestHandler extends Dispatcher {

    private RequestListener listener = null;
    private HashMap<Integer, BaseDataServer> servers;


    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        TestLogger.log("get request " + request.toString());
        MockResponse mockResponse = new MockResponse();
        int requestType = 0;
        Object requestArg = null;
        BaseDataServer server = null;
        boolean handled = false;
        for (Map.Entry<Integer, BaseDataServer> entry : servers.entrySet()) {
            if (entry.getValue().isMyBusinessRequest(request)) {
                TestLogger.log("handle by " + entry.getValue().toString());
                requestType = entry.getKey();
                requestArg = entry.getValue().getRequestArg(request);
                mockResponse = entry.getValue().handle(request);
                server = entry.getValue();
                handled = true;
                break;
            }
        }
        if (!handled) {
            TestLogger.log("NOT HANDLED!!! do you forget to add server?");
        }
        if (listener != null) {
            listener.onRequest(requestType, requestArg, server);
        }
        return mockResponse;
    }

    public void listenRequest(RequestListener listener) {
        this.listener = listener;
    }

    public void setServers(HashMap<Integer, BaseDataServer> servers) {
        this.servers = servers;
    }

}
