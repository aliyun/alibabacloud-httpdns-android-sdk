package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.probe.ProbeTask;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;
import com.alibaba.sdk.android.httpdns.test.server.base.RequestListener;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import static com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer.REQUEST_TYPE_INTERPRET_HOST;
import static org.mockito.Mockito.mock;

/**
 * 模拟测试速度的服务器
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class MockSpeedTestServer implements ProbeTask.SpeedTestSocketFactory, RequestListener {

    private int createCount = 0;
    private ArrayList<String[]> sortedIps = new ArrayList<>();
    private HashMap<String, String[]> sortedHostIpsMap = new HashMap<>();

    @Override
    public Socket create() {
        Socket socket = mock(Socket.class);
        String[] sortedIps = getSortedIpsFromCount(createCount++);
        UnitTestUtil.setSpeedSort(socket, sortedIps);
        return socket;
    }

    @Override
    public void onRequest(int type, Object arg, BaseDataServer server) {
        // InterpretHostRequest
        if (type == REQUEST_TYPE_INTERPRET_HOST) {
            String host = ((InterpretHostServer.InterpretHostArg) arg).host;
            String[] ips = ((InterpretHostServer) server).getResponse((InterpretHostServer.InterpretHostArg) arg, 1, false).get(0).getIps();
            String[] sorted = UnitTestUtil.changeArraySort(ips);
            sortedIps.add(sorted);
            sortedHostIpsMap.put(host, sorted);
        }
    }


    private String[] getSortedIpsFromCount(int index) {
        int sum = 0;
        for (int i = 0; i < sortedIps.size(); i++) {
            if (index >= sum && index < sum + sortedIps.get(i).length) {
                return sortedIps.get(i);
            }
        }
        return new String[0];
    }

    /**
     * 监听httpdns服务的返回
     *
     * @param server
     */
    public void watch(HttpDnsServer server) {
        server.addRequestListener(this);
    }

    /**
     * 停止服务
     */
    public void stop() {
        this.sortedIps.clear();
        this.sortedHostIpsMap.clear();
    }

    /**
     * 返回根据服务器速度排序的ip
     *
     * @param requestHost
     * @return
     */
    public String[] getSortedIpsFor(String requestHost) {
        return sortedHostIpsMap.get(requestHost);
    }
}
