package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;
import com.alibaba.sdk.android.httpdns.test.server.base.RequestHandler;
import com.alibaba.sdk.android.httpdns.test.server.base.RequestListener;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.mockwebserver.MockWebServer;

/**
 * 模拟httpdns服务
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public class HttpDnsServer {
    public static final SecretService secretServce = new SecretService();
    public static final int REQUEST_TYPE_INTERPRET_HOST = 1;
    public static final int REQUEST_TYPE_UPDATE_SERVER_IPS = 2;
    public static final int REQUEST_TYPE_DEBUG_REQUEST = 3;
    public static final int REQUEST_TYPE_RESOLVE_REQUEST = 4;
    private MockWebServer mockWebServer = new MockWebServer();
    private RequestHandler requestHandler = new RequestHandler();

    private HashMap<Integer, BaseDataServer> servers = new HashMap<>();
    private InterpretHostServer interpretHostServer;
    private ResolveHostServer resolveHostServer;
    private ServerIpsServer serverIpsServer;
    private DebugApiServer debugApiServer;

    /**
     * 服务启动
     */
    public void start() {
        interpretHostServer = new InterpretHostServer(secretServce);
        resolveHostServer = new ResolveHostServer(secretServce);
        serverIpsServer = new ServerIpsServer();
        debugApiServer = new DebugApiServer();
        servers.put(REQUEST_TYPE_INTERPRET_HOST, interpretHostServer);
        servers.put(REQUEST_TYPE_UPDATE_SERVER_IPS, serverIpsServer);
        servers.put(REQUEST_TYPE_DEBUG_REQUEST, debugApiServer);
        servers.put(REQUEST_TYPE_RESOLVE_REQUEST, resolveHostServer);
        try {
            mockWebServer.start();
            mockWebServer.setDispatcher(requestHandler);
            requestHandler.setServers(servers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TestLogger.log(this.toString() + " start " + mockWebServer.toString());
    }

    /**
     * 服务停止
     */
    public void stop() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取服务IP地址
     *
     * @return
     */
    public String getServerIp() {
        return mockWebServer.getHostName();
    }

    /**
     * 获取服务端口
     *
     * @return
     */
    public int getPort() {
        return mockWebServer.getPort();
    }

    /**
     * 设置请求监听
     *
     * @param requestListener
     */
    public void addRequestListener(RequestListener requestListener) {
        this.requestHandler.listenRequest(requestListener);
    }

    public InterpretHostServer getInterpretHostServer() {
        return interpretHostServer;
    }

    public ServerIpsServer getServerIpsServer() {
        return serverIpsServer;
    }

    public DebugApiServer getDebugApiServer() {
        return debugApiServer;
    }

    public ResolveHostServer getResolveHostServer() {
        return resolveHostServer;
    }

    public String createSecretFor(String account) {
        return secretServce.get(account);
    }
}
