package com.alibaba.sdk.android.httpdns.version;

import android.Manifest;

import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 2.3.0 版本中关于初始IP配置的需求， 因为和一般case的条件不同，所以单独列出
 * 1. 默认服务可以不是 中国大陆
 * 2. 增加默认调度IP，即在当前服务IP调度失败，初始服务IP调度失败时，使用默认的调度IP，此能力是应用国际版初始IP不稳定的情况
 *
 * @author zonglin.nzl
 * @date 8/26/22
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class V2_3_0_config {

	private HttpDnsServer server = new HttpDnsServer();
	private HttpDnsServer server1 = new HttpDnsServer();
	private HttpDnsServer server2 = new HttpDnsServer();

	private MockSpeedTestServer speedTestServer = new MockSpeedTestServer();
	private ILogger logger;

	private String REGION_DEFAULT = "sg";
	private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(10));

	@Before
	public void setUp() {
		// 设置日志接口
		HttpDnsLog.enable(true);
		logger = new ILogger() {
			private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

			@Override
			public void log(String msg) {
				System.out.println(
					"[" + format.format(new Date()) + "][Httpdns][" + System.currentTimeMillis() % (
						60 * 1000) + "]" + msg);
			}
		};
		HttpDnsLog.setLogger(logger);
		// 重置实例
		HttpDns.resetInstance();
		// 重置配置
		InitConfig.removeConfig(null);
		// 这里我们启动3个 服务节点用于测试
		server.start();
		server1.start();
		server2.start();
		ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
		application.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE);

		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
	}

	@After
	public void tearDown() {
		HttpDnsLog.removeLogger(logger);
		server.stop();
		server1.stop();
		server2.stop();
		speedTestServer.stop();
	}

	/**
	 * 默认region可以是其它region
	 * 通过修改region配置，验证初始化时，使用的region不同
	 */
	@Test
	public void testDefaultRegion() {
		app.start(true);
	}

	/**
	 * 更新服务IP时，包括 初始化时，和后续修改region时 如果前面的服务失败，能够回退到默认的服务IP进行调度请求
	 */
	@Test
	public void testUpdateServerFallbackToDefaultUpdateServer() {
		// 初始服务 调度失败
		server.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, 500, "whatever", -1);
		// 第一个默认调度服务 也失败
		server1.getServerIpsServer().preSetRequestResponse(REGION_DEFAULT, 500, "whatever", -1);

		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server},
			new HttpDnsServer[] {server1, server2});
		// 初始化httpdns 初始化调度请求
		app.start(false);

		app.waitForAppThread();
		assertThat("调度请求失败会回退到默认调度IP请求, region " + REGION_DEFAULT,
			server2.getServerIpsServer().hasRequestForArg(REGION_DEFAULT, 1, true));
	}

}
