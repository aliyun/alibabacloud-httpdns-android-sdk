package com.alibaba.sdk.android.httpdns.version;

import android.Manifest;
import android.net.ConnectivityManager;

import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.InterpretHostServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.server.ServerIpsServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 支持网络切换后，有很多逻辑需要调整，包括
 * 0. 调度请求，同时更新ipv4和ipv6 服务节点
 * 1. ipv6 only 场景下，调度使用ipv6服务节点
 * 2. ipv6 only 场景下，解析使用ipv6服务节点
 * 2.1 ipv6 only 场景下，服务节点的缓存和再次使用
 * 3. ipv4 切换 ipv6后，切换使用ipv6的服务节点解析 调度
 * 4. ipv6 切换 ipv4后，切换使用ipv4的服务节点解析 调度
 * 5. 解析时，根据网络类型判断解析哪种ip
 *
 * @author zonglin.nzl
 * @date 8/29/22
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class V2_3_0_NetType {


    private final String REGION_DEFAULT = Constants.REGION_MAINLAND;
    private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(20));

    private HttpDnsServer serverV4One = new HttpDnsServer();
    private HttpDnsServer serverV4Two = new HttpDnsServer();
    private HttpDnsServer serverV4Three = new HttpDnsServer();

    private HttpDnsServer serverV6One = new HttpDnsServer();
    private HttpDnsServer serverV6Two = new HttpDnsServer();
    private HttpDnsServer serverV6Three = new HttpDnsServer();

    private MockSpeedTestServer speedTestServer = new MockSpeedTestServer();
    private ILogger logger;

    @Before
    public void setUp() {
        // 设置日志接口
        HttpDnsLog.enable(true);
        logger = new ILogger() {
            private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

            @Override
            public void log(String msg) {
                System.out.println("[" + format.format(new Date()) + "][Httpdns][" + System.currentTimeMillis() % (60 * 1000) + "]" + msg);
            }
        };
        HttpDnsLog.setLogger(logger);
        // 重置实例
        HttpDns.resetInstance();
        // 重置配置
        InitConfig.removeConfig(null);
        // 这里我们启动3个 服务节点用于测试
        serverV4One.start();
        serverV4Two.start();
        serverV4Three.start();
        serverV6One.start(true);
        serverV6Two.start(true);
        serverV6Three.start(true);
        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE);

        // 内置第一个服务节点，第二、三个服务节点作为调度请求的结果
        String response = ServerIpsServer.createUpdateServerResponse(
                new String[]{serverV4Two.getServerIp(), serverV4Three.getServerIp()},
                new String[]{serverV6Two.getServerIp(), serverV6Three.getServerIp()},
                new int[]{serverV4Two.getPort(), serverV4Three.getPort()},
                new int[]{serverV6Two.getPort(), serverV6Three.getPort()});

        serverV4One.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);
        serverV4Two.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);
        serverV4Three.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);

        serverV6One.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);
        serverV6Two.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);
        serverV6Three.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, HttpURLConnection.HTTP_OK, response, -1);

        app.configInitServer(REGION_DEFAULT, new HttpDnsServer[]{serverV4One}, null);
        app.configIpv6InitServer(new HttpDnsServer[]{serverV6One}, null);
        app.configSpeedTestSever(speedTestServer);
    }

    @After
    public void tearDown() {
        HttpDnsLog.removeLogger(logger);
        app.waitForAppThread();
        app.stop();
        serverV4One.stop();
        serverV4Two.stop();
        serverV4Three.stop();
        serverV6One.stop();
        serverV6Two.stop();
        serverV6Three.stop();
        speedTestServer.stop();
    }


    /**
     * 启动更新服务IP， ipv4
     */
    @Test
    public void testUpdateServerWhenStart() {

        // v4 网络情况下启动
        app.changeNetType(NetType.v4);
        app.start(false);

        ServerStatusHelper.hasReceiveRegionChange("v4网络下使用内置v4节点更新服务", app, serverV4One, REGION_DEFAULT, true);

        // 随便发起一个请求
        app.requestInterpretHost();
        // 确认服务节点是否更新成功
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("更新后，使用新服务节点解析", app, serverV4Two, 1);
    }

    /**
     * 启动更新服务IP， ipv6
     */
    @Test
    public void testUpdateServerWhenStartUnderV6() {

        // v6 网络情况下启动
        app.changeNetType(NetType.v6);
        app.start(false);

        ServerStatusHelper.hasReceiveRegionChange("v6网络下使用内置v6节点更新服务", app, serverV6One, REGION_DEFAULT, true);

        // 随便发起一个请求
        app.requestInterpretHost();
        // 确认服务节点是否更新成功
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("更新后，使用新服务节点解析", app, serverV6Two, 1);
    }

    /**
     * 启动更新服务IP， both
     */
    @Test
    public void testUpdateServerWhenStartUnderV4V6() {

        // 都支持 网络情况下启动
        app.changeNetType(NetType.both);
        app.start(false);

        ServerStatusHelper.hasReceiveRegionChange("都支持的网络下使用内置v4节点更新服务", app, serverV4One, REGION_DEFAULT, true);

        // 随便发起一个请求
        app.requestInterpretHost();
        // 确认服务节点是否更新成功
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("更新后，使用新服务节点解析", app, serverV4Two, 1);
    }

    /**
     * 测试ipv6服务节点的缓存
     */
    @Test
    public void testCacheIpv6ServerIps() {

        // 先利用此case获取ipv6的服务节点
        testUpdateServerWhenStartUnderV6();

        String host = RandomValue.randomHost();
        serverV6Two.getInterpretHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        // 发起一次请求，使切换服务节点
        app.requestInterpretHost(host);
        app.waitForAppThread();

        assertThat("第一次失败后，切换服务IP重试", serverV6Three.getInterpretHostServer().hasRequestForArg(host, 1, true));

        // 重置
        HttpDns.resetInstance();
        serverV6One.getServerIpsServer().cleanRecord();
        serverV6One.getInterpretHostServer().cleanRecord();
        serverV6Two.getServerIpsServer().cleanRecord();
        serverV6Two.getInterpretHostServer().cleanRecord();
        serverV6Three.getServerIpsServer().cleanRecord();
        serverV6Three.getInterpretHostServer().cleanRecord();

        // 模拟第二次启动
        app.changeNetType(NetType.v6);
        app.start(false);

        String anotherHost = RandomValue.randomHost();
        app.requestInterpretHost(anotherHost);
        app.waitForAppThread();

        assertThat("启动时，从缓存中读取服务节点使用", serverV6Three.getInterpretHostServer().hasRequestForArg(anotherHost, 1, true));
    }


    /**
     * 网络变化时，切换对应的服务节点
     */
    @Test
    @Config(shadows = {ShadowNetworkInfo.class})
    public void testChangeServerIpWhenNetChange() {

        //复用case 获取测试环境， 当前应该是 v6网络，serverV6Three 是当前服务节点，v4的服务节点应该是serverV4Two
        testCacheIpv6ServerIps();

        // 网络变化到v4 wifi
        app.changeNetType(NetType.v4);
        app.changeToNetwork(ConnectivityManager.TYPE_WIFI);

        String host1 = RandomValue.randomHost();
        app.requestInterpretHost(host1);
        app.waitForAppThread();

        assertThat("网络环境变为v4后使用v4的服务节点", serverV4Two.getInterpretHostServer().hasRequestForArg(host1, 1, true));


        // 网络变化到v6 mobile
        app.changeNetType(NetType.v6);
        app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

        String host2 = RandomValue.randomHost();
        app.requestInterpretHost(host2);
        app.waitForAppThread();

        assertThat("网络环境变为v6后使用v6的服务节点", serverV6Three.getInterpretHostServer().hasRequestForArg(host2, 1, true));

        // 网络变化到都支持 wifi
        app.changeNetType(NetType.both);
        app.changeToNetwork(ConnectivityManager.TYPE_WIFI);

        String host3 = RandomValue.randomHost();
        app.requestInterpretHost(host3);
        app.waitForAppThread();

        assertThat("网络环境变为both后使用v4的服务节点", serverV4Two.getInterpretHostServer().hasRequestForArg(host3, 1, true));
    }


    @Test
    public void testAutoRequestIpTypeForIpv4() {

        // 复用case 获取v4网络状态
        testUpdateServerWhenStart();
        serverV4Two.getInterpretHostServer().cleanRecord();

        String host = RandomValue.randomHost();
        app.requestInterpretHost(host, RequestIpType.auto);
        app.waitForAppThread();

        assertThat("当前网络仅支持v4时，自动解析只会解析v4类型", serverV4Two.getInterpretHostServer().hasRequestForArg(host, 1, true));
    }

    @Test
    public void testAutoRequestIpTypeForIpv6() {
        // 复用case 获取v6网络状态
        testUpdateServerWhenStartUnderV6();
        serverV6Two.getInterpretHostServer().cleanRecord();

        String host = RandomValue.randomHost();
        app.requestInterpretHost(host, RequestIpType.auto);
        app.waitForAppThread();

        assertThat("当前网络仅支持v6时，自动解析只会解析v6类型", serverV6Two.getInterpretHostServer().hasRequestForArg(InterpretHostServer.InterpretHostArg.create(host, RequestIpType.v6), 1, true));
    }

    @Test
    public void testAutoRequestIpTypeForBoth() {
        // 复用case 获取v4v6网络状态
        testUpdateServerWhenStartUnderV4V6();
        serverV4Two.getInterpretHostServer().cleanRecord();

        String host = RandomValue.randomHost();
        app.requestInterpretHost(host, RequestIpType.auto);
        app.waitForAppThread();

        assertThat("当前网络支持v4v6时，自动解析会解析v4v6类型", serverV4Two.getInterpretHostServer().hasRequestForArg(InterpretHostServer.InterpretHostArg.create(host, RequestIpType.both), 1, true));
    }
}
