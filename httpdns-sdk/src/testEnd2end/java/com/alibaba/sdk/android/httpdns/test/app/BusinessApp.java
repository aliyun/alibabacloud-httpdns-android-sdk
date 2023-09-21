package com.alibaba.sdk.android.httpdns.test.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.sdk.android.httpdns.ApiForTest;
import com.alibaba.sdk.android.httpdns.BeforeHttpDnsServiceInit;
import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitManager;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 模拟业务app
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public class BusinessApp {
    private String accountId;
    private String secret;
    private String businessHost = RandomValue.randomHost();
    private HttpDnsService httpDnsService;
    private ILogger mockLogger;

    private String initRegion;
    private HttpDnsServer[] initServers;
    private HttpDnsServer[] defaultUpdateServers;
    private HttpDnsServer[] initServersIpv6;
    private HttpDnsServer[] defaultUpdateServersIpv6;
    private MockSpeedTestServer speedTestServer;

    private TestExecutorService testExecutorService;

    private NetType currentNetType = NetType.v4;

    public BusinessApp(String accountId) {
        this.accountId = accountId;
    }

    public BusinessApp(String account, String secret) {
        this.accountId = account;
        this.secret = secret;
    }

    public void configInitServer(final String region, final HttpDnsServer[] initServers, final HttpDnsServer[] defaultUpdateServers) {
        this.initRegion = region == null ? Constants.REGION_MAINLAND : region;
        this.initServers = initServers;
        this.defaultUpdateServers = defaultUpdateServers;
    }

    public void configInitRegion(final String region) {
        this.initRegion = region == null ? Constants.REGION_MAINLAND : region;
    }

    public void configIpv6InitServer(final HttpDnsServer[] initServers, final HttpDnsServer[] defaultUpdateServers) {
        this.initServersIpv6 = initServers;
        this.defaultUpdateServersIpv6 = defaultUpdateServers;
    }

    public void configSpeedTestSever(MockSpeedTestServer speedTestServer) {
        this.speedTestServer = speedTestServer;
    }

    /**
     * 应用启动
     */
    public void start(boolean removeInitUpdateServerRecord) {

        InitManager.getInstance().add(accountId, new BeforeHttpDnsServiceInit() {
            @Override
            public void beforeInit(HttpDnsService httpDnsService) {
                // 设置针对测试的辅助接口
                if (httpDnsService instanceof ApiForTest) {
                    if (BusinessApp.this.initServers != null) {
                        String[] ips = new String[BusinessApp.this.initServers.length];
                        int[] ports = new int[BusinessApp.this.initServers.length];
                        for (int i = 0; i < BusinessApp.this.initServers.length; i++) {
                            ips[i] = BusinessApp.this.initServers[i].getServerIp();
                            ports[i] = BusinessApp.this.initServers[i].getPort();
                        }

                        String[] ipv6s = null;
                        int[] v6ports = null;
                        if (BusinessApp.this.initServersIpv6 != null) {
                            ipv6s = new String[BusinessApp.this.initServersIpv6.length];
                            v6ports = new int[BusinessApp.this.initServersIpv6.length];
                            for (int i = 0; i < BusinessApp.this.initServersIpv6.length; i++) {
                                ipv6s[i] = BusinessApp.this.initServersIpv6[i].getServerIp();
                                v6ports[i] = BusinessApp.this.initServersIpv6[i].getPort();
                            }
                        }

                        // 设置初始IP
                        ((ApiForTest) httpDnsService).setInitServer(initRegion, ips, ports, ipv6s, v6ports);
                    }
                    if (BusinessApp.this.defaultUpdateServers != null) {
                        String[] defaultServerIps = new String[BusinessApp.this.defaultUpdateServers.length];
                        int[] ports = new int[BusinessApp.this.defaultUpdateServers.length];
                        for (int i = 0; i < BusinessApp.this.defaultUpdateServers.length; i++) {
                            defaultServerIps[i] = BusinessApp.this.defaultUpdateServers[i].getServerIp();
                            ports[i] = BusinessApp.this.defaultUpdateServers[i].getPort();
                        }
                        ((ApiForTest) httpDnsService).setDefaultUpdateServer(defaultServerIps, ports);
                    }

                    if (BusinessApp.this.defaultUpdateServersIpv6 != null) {
                        String[] defaultServerIps = new String[BusinessApp.this.defaultUpdateServersIpv6.length];
                        int[] ports = new int[BusinessApp.this.defaultUpdateServersIpv6.length];
                        for (int i = 0; i < BusinessApp.this.defaultUpdateServersIpv6.length; i++) {
                            defaultServerIps[i] = BusinessApp.this.defaultUpdateServersIpv6[i].getServerIp();
                            ports[i] = BusinessApp.this.defaultUpdateServersIpv6[i].getPort();
                        }
                        ((ApiForTest) httpDnsService).setDefaultUpdateServerIpv6(defaultServerIps, ports);
                    }
                    testExecutorService = new TestExecutorService(((ApiForTest) httpDnsService).getWorker());
                    ((ApiForTest) httpDnsService).setThread(testExecutorService);
                    if (BusinessApp.this.speedTestServer != null) {
                        ((ApiForTest) httpDnsService).setSocketFactory(BusinessApp.this.speedTestServer);
                    }
                    ((ApiForTest) httpDnsService).setNetworkDetector(new HttpDnsSettings.NetworkDetector() {
                        @Override
                        public NetType getNetType(Context context) {
                            return currentNetType;
                        }
                    });
                }
            }
        });
        // 获取httpdns
        if (secret == null) {
            httpDnsService = HttpDns.getService(RuntimeEnvironment.application, accountId);
        } else {
            httpDnsService = HttpDns.getService(RuntimeEnvironment.application, accountId, secret);
        }
        assertThat("HttpDns.getService should not return null", httpDnsService, notNullValue());

        for (int i = 0; i < BusinessApp.this.initServers.length; i++) {
            TestLogger.log(this.toString() + " start with " + BusinessApp.this.initServers[i].toString());
        }
        if (removeInitUpdateServerRecord) {
            ServerStatusHelper.hasReceiveRegionChange("初始化时会更新一次服务节点", this, BusinessApp.this.initServers[0], initRegion, true);
        }
    }

    /**
     * 应用退出
     */
    public void stop() {
        testExecutorService.shutdownNow();
        httpDnsService = null;
    }


    /**
     * 获取app使用httpdns accountId
     *
     * @return
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * 获取业务使用host
     *
     * @return
     */
    public String getRequestHost() {
        return businessHost;
    }

    /**
     * 等待app的线程执行完毕
     */
    public void waitForAppThread() {
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析域名
     *
     * @return
     */
    public String[] requestInterpretHost() {
        return httpDnsService.getIpsByHostAsync(businessHost);
    }

    /**
     * 解析ipv6的域名
     *
     * @return
     */
    public String[] requestInterpretHostForIpv6() {
        return httpDnsService.getIPv6sByHostAsync(businessHost);
    }

    /**
     * 解析域名
     *
     * @param host
     * @return
     */
    public String[] requestInterpretHost(String host) {
        return httpDnsService.getIpsByHostAsync(host);
    }

    /**
     * 解析ipv6的域名
     *
     * @param requestHost
     * @return
     */
    public String[] requestInterpretHostForIpv6(String requestHost) {
        return httpDnsService.getIPv6sByHostAsync(requestHost);
    }

    /**
     * 指定类型解析
     *
     * @param host
     * @param type
     * @return
     */
    public HTTPDNSResult requestInterpretHost(String host, RequestIpType type) {
        return httpDnsService.getIpsByHostAsync(host, type, null, null);
    }

    /**
     * 设置日志接口
     */
    public void setLogger() {
        mockLogger = mock(ILogger.class);
        HttpDnsLog.setLogger(mockLogger);
        httpDnsService.setLogger(mockLogger);
    }


    public void removeLogger() {
        HttpDnsLog.removeLogger(mockLogger);
    }


    /**
     * check 收到或者没有收到日志
     *
     * @param received
     */
    public void hasReceiveLogInLogcat(boolean received) {
        List<ShadowLog.LogItem> list = ShadowLog.getLogs();
        if (received) {
            assertThat(list.size(), greaterThan(1));
        } else {
            assertThat(list.size(), is(0));
        }
        ShadowLog.clear();
    }

    /**
     * check logger收到日志
     */
    public void hasReceiveLogInLogger() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, atLeastOnce()).log(logArgument.capture());
        assertThat(logArgument.getAllValues().size(), greaterThan(1));
    }

    /**
     * check logger收到日志
     */
    public void hasReceiveLogInLogger(String logKey) {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, atLeastOnce()).log(logArgument.capture());
        assertThat(logArgument.getAllValues().size(), greaterThan(1));
        assertThat("有特定的日志" + logKey, stringListContain(logArgument.getAllValues(), logKey));
    }

    private boolean stringListContain(List<String> list, String msg) {
        for (String tmp : list) {
            if (tmp.contains(msg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 切换region
     *
     * @param region
     */
    public void changeRegionTo(String region) {
        httpDnsService.setRegion(region);
    }

    /**
     * 设置ip probe
     */
    public void enableIpProbe() {
        ArrayList<IPProbeItem> list = new ArrayList<>();
        list.add(new IPProbeItem(businessHost, 6666));
        httpDnsService.setIPProbeList(list);
    }

    /**
     * 设置请求超时
     *
     * @param ms
     */
    public void setTimeout(int ms) {
        httpDnsService.setTimeoutInterval(ms);
    }

    /**
     * 清除缓存
     *
     * @param hosts
     */
    public void cleanHostCache(ArrayList<String> hosts) {
        httpDnsService.cleanHostCache(hosts);
    }

    /**
     * 设置更新服务IP的最小间隔
     *
     * @param timeInterval
     */
    public void setUpdateServerTimeInterval(int timeInterval) {
        if (httpDnsService instanceof ApiForTest) {
            ((ApiForTest) httpDnsService).setUpdateServerTimeInterval(timeInterval);
        }
    }

    /**
     * 设置嗅探模式的最小间隔
     *
     * @param timeInterval
     */
    public void setSniffTimeInterval(int timeInterval) {
        if (httpDnsService instanceof ApiForTest) {
            ((ApiForTest) httpDnsService).setSniffTimeInterval(timeInterval);
        }
    }

    /**
     * check 相同的account是否返回相同的实例
     */
    public void checkSameInstanceForSameAcount() {
        assertThat("一个accountId对应一个实例", httpDnsService == HttpDns.getService(RuntimeEnvironment.application, accountId));
    }

    public HTTPDNSResult requestSDNSInterpretHost(HashMap<String, String> extras, String cacheKey) {
        return httpDnsService.getIpsByHostAsync(businessHost, extras, cacheKey);
    }

    public void setGlobalParams(HashMap<String, String> globalParams) {
        httpDnsService.setSdnsGlobalParams(globalParams);
    }

    public void cleanGlobalParams() {
        httpDnsService.clearSdnsGlobalParams();
    }

    public HTTPDNSResult requestSDNSInterpretHostForIpv6(HashMap<String, String> extras, String cacheKey) {
        return httpDnsService.getIpsByHostAsync(businessHost, RequestIpType.v6, extras, cacheKey);
    }

    public void preInterpreHost(ArrayList<String> hostList, RequestIpType type) {
        httpDnsService.setPreResolveHosts(hostList, type);
    }

    public void enableExpiredIp(boolean enable) {
        httpDnsService.setExpiredIPEnabled(enable);
    }

    public void enableCache(boolean clean) {
        httpDnsService.setCachedIPEnabled(true, clean);
        waitForAppThread();
    }

    public void setFilter() {
        httpDnsService.setDegradationFilter(new DegradationFilter() {
            @Override
            public boolean shouldDegradeHttpDNS(String hostName) {
                return hostName.equals(businessHost);
            }
        });
    }

    public void setTime(long time) {
        httpDnsService.setAuthCurrentTime(time);
    }

    public void enableResolveAfterNetworkChange(boolean enable) {
        httpDnsService.setPreResolveAfterNetworkChanged(enable);
    }

    public void changeToNetwork(int netType) {
        ShadowConnectivityManager shadowConnectivityManager = Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo;
        if (netType == -1) {
            networkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.DISCONNECTED, netType, 0, true, false);
        } else {
            networkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, netType, 0, true, true);
        }
        shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
        RuntimeEnvironment.application.sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        ShadowLooper.runUiThreadTasks();
        waitForAppThread();
        TestLogger.log("change network to " + netType);
    }

    @SuppressLint("ApplySharedPref")
    public void changeServerIpUpdateTimeTo(long time) {
        SharedPreferences.Editor editor = RuntimeEnvironment.application.getSharedPreferences(Constants.CONFIG_CACHE_PREFIX + accountId, Context.MODE_PRIVATE).edit();
        editor.putLong("servers_last_updated_time", time);
        editor.commit();
    }

    public void enableHttps(boolean enableHttps) {
        httpDnsService.setHTTPSRequestEnabled(enableHttps);
    }

    public String[] requestInterpretHostSync(String host) {
        if (httpDnsService instanceof SyncService) {
            return ((SyncService) httpDnsService).getByHost(host, RequestIpType.v4).getIps();
        }
        return new String[0];
    }

    public HTTPDNSResult requestInterpretHostSync(String host, RequestIpType type) {
        if (httpDnsService instanceof SyncService) {
            return ((SyncService) httpDnsService).getByHost(host, type);
        }
        return HTTPDNSResult.empty(host);
    }

    public void checkThreadCount(boolean check) {
        if (httpDnsService instanceof ApiForTest) {
            if (((ApiForTest) httpDnsService).getWorker() instanceof TestExecutorService) {
                ((TestExecutorService) ((ApiForTest) httpDnsService).getWorker()).enableThreadCountCheck(check);
            }
        }
    }

    public void changeNetType(NetType netType) {
        currentNetType = netType;
    }
}
