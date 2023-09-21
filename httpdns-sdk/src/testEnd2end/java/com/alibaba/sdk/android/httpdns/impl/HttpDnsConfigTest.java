package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.SyncExecutorService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class HttpDnsConfigTest {
    private HttpDnsConfig config;
    private String account = RandomValue.randomStringWithFixedLength(6);
    private String[] serverIps = RandomValue.randomIpv4s();
    private int[] ports = RandomValue.randomPorts();

    private final String REGION_DEFAULT = Constants.REGION_MAINLAND;

    @Before
    public void setUp() {
        config = new HttpDnsConfig(RuntimeEnvironment.application, account);
        config.setWorker(new SyncExecutorService());
        config.setInitServers(REGION_DEFAULT, serverIps, null, null, null);
    }

    @Test
    public void setServerIpsWillResetServerStatus() {
        config.getCurrentServer().shiftServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());
        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());
        MatcherAssert.assertThat("current ok server is " + serverIps[1], config.getCurrentServer().getServerIp().equals(serverIps[1]));
        String[] newServerIps = RandomValue.randomIpv4s();
        config.getCurrentServer().setServerIps(null, newServerIps, null, null, null);
        MatcherAssert.assertThat("current ok server is " + newServerIps[0], config.getCurrentServer().getServerIp().equals(newServerIps[0]));
    }

    @Test
    public void shiftServerTest() {
        config.getCurrentServer().setServerIps(null, serverIps, ports, null, null);
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getCurrentServer().getServerIp()));
        MatcherAssert.assertThat("default index is first", ports[0] == config.getCurrentServer().getPort());

        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        checkShiftResult(1);

        checkShiftResult(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult(0));

        checkShiftResult(1);

        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        checkShiftResult(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult(1));
    }

    private boolean checkShiftResult(int i) {
        boolean isBackToFirst = config.getCurrentServer().shiftServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getCurrentServer().getServerIp()));
        MatcherAssert.assertThat("shift server", ports[i] == config.getCurrentServer().getPort());

        return isBackToFirst;
    }


    @Test
    public void shiftServerTest2() {
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getCurrentServer().getServerIp()));

        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        checkShiftResult2(1);

        checkShiftResult2(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult2(0));

        checkShiftResult2(1);

        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        checkShiftResult2(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult2(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult2(1));
    }

    private boolean checkShiftResult2(int i) {
        boolean isBackToFirst = config.getCurrentServer().shiftServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getCurrentServer().getServerIp()));

        return isBackToFirst;
    }

    @Test
    public void markCurrentServerSuccess() {
        MatcherAssert.assertThat("current Server can mark", config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort()));
    }

    @Test
    public void markOtherServerFail() {
        String ip = config.getCurrentServer().getServerIp();
        int port = config.getCurrentServer().getPort();
        config.getCurrentServer().shiftServer(ip, port);
        MatcherAssert.assertThat("only current Server can mark", !config.getCurrentServer().markOkServer(ip, port));
    }

    @Test
    public void setSameServerWillReturnFalse() {
        config.getCurrentServer().setServerIps(null, serverIps, ports, null, null);
        MatcherAssert.assertThat("same server will return false", !config.getCurrentServer().setServerIps(null, serverIps, ports, null, null));
    }

    @Test
    public void testConfigCache() {
        config.getCurrentServer().shiftServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());
        config.getCurrentServer().markOkServer(config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort());

        HttpDnsConfig another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getCurrentServer().getServerIp(), Matchers.equalTo(config.getCurrentServer().getServerIp()));
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getCurrentServer().getPort(), Matchers.equalTo(config.getCurrentServer().getPort()));

        config.getCurrentServer().setServerIps(null, RandomValue.randomIpv4s(), RandomValue.randomPorts(), null, null);

        another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getCurrentServer().getServerIp(), Matchers.equalTo(config.getCurrentServer().getServerIp()));
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getCurrentServer().getPort(), Matchers.equalTo(config.getCurrentServer().getPort()));
    }

    @Test
    public void testRegionUpdate() {

        String otherRegion = Constants.REGION_HK;

        // 这一条验证和HttpDnsConfig无关
        MatcherAssert.assertThat("默认region是通过gradle配置的", Constants.REGION_DEFAULT, Matchers.is(Matchers.equalTo(BuildConfig.DEFAULT_REGION)));

        // HttpDnsConfig默认的region应该和测试方法setInitServer的值一致
        MatcherAssert.assertThat("默认region正确", config.getRegion(), Matchers.is(Matchers.equalTo(REGION_DEFAULT)));
        MatcherAssert.assertThat("默认服务节点的region是默认region", config.getCurrentServer().getRegion(), Matchers.is(Matchers.equalTo(REGION_DEFAULT)));
        MatcherAssert.assertThat("默认配置是region匹配", config.isCurrentRegionMatch(), Matchers.is(true));

        String regionBak = config.getRegion();
        config.setRegion(otherRegion);

        MatcherAssert.assertThat("setRegion 更新成功", config.getRegion(), Matchers.is(Matchers.equalTo(otherRegion)));
        MatcherAssert.assertThat("setRegion不影响服务节点的region", config.getCurrentServer().getRegion(), Matchers.is(Matchers.equalTo(regionBak)));
        MatcherAssert.assertThat("setRegion 导致region不匹配", config.isCurrentRegionMatch(), Matchers.is(false));

        config.getCurrentServer().setServerIps(otherRegion, RandomValue.randomIpv4s(), RandomValue.randomPorts(), null, null);

        MatcherAssert.assertThat("setServerIps更像服务节点", config.getCurrentServer().getRegion(), Matchers.is(Matchers.equalTo(otherRegion)));
        MatcherAssert.assertThat("服务节点更新后，region匹配", config.isCurrentRegionMatch(), Matchers.is(true));

        regionBak = config.getRegion();
        config.setRegion(REGION_DEFAULT);

        MatcherAssert.assertThat("setRegion 更新成功", config.getRegion(), Matchers.is(Matchers.equalTo(REGION_DEFAULT)));
        MatcherAssert.assertThat("setRegion不影响服务节点的region", config.getCurrentServer().getRegion(), Matchers.is(Matchers.equalTo(regionBak)));
        MatcherAssert.assertThat("setRegion 导致region不匹配", config.isCurrentRegionMatch(), Matchers.is(false));

        config.getCurrentServer().setServerIps(REGION_DEFAULT, RandomValue.randomIpv4s(), RandomValue.randomPorts(), null, null);

        MatcherAssert.assertThat("setServerIps更新服务节点", config.getCurrentServer().getRegion(), Matchers.is(Matchers.equalTo(REGION_DEFAULT)));
        MatcherAssert.assertThat("服务节点更新后，region匹配", config.isCurrentRegionMatch(), Matchers.is(true));
    }

}
