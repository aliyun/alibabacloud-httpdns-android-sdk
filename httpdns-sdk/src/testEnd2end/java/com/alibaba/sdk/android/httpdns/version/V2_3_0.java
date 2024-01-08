package com.alibaba.sdk.android.httpdns.version;

import android.Manifest;
import android.net.ConnectivityManager;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.resolve.BatchResolveHostResponse;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.ResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.server.BatchResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * HTTPDNS 2.3.0 版本需求
 * 1. 缓存使用的ttl改为可配置
 * 2. 主站域名的ip不经常变动，单独处理相关逻辑
 * 3. 没有解析结果的域名解析，算是一种无效请求，也按主站域名处理。因为没有解析结果，也可以认为是一种固定的解析结果
 * 3.1 但是这里有一种特殊情况，即同时解析v4、v6的情况，有可能v6是无效的，而v4是有效的，此时需要根据缓存把解析改为仅解析v4
 * 4. 缓存有效时，过滤掉相关的解析，比如 解析v4 v6， v4有效，就只解析v6 反之亦然
 * 5. 测试异常情况下的反应，包括 日志输出 是否重试 是否切换服务IP 是否生成一个空缓存
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class V2_3_0 {

	private final String REGION_DEFAULT = "sg";
	private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(20));

	private HttpDnsServer server = new HttpDnsServer();
	private HttpDnsServer server1 = new HttpDnsServer();
	private HttpDnsServer server2 = new HttpDnsServer();

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
		app.start(true);
	}

	@After
	public void tearDown() {
		HttpDnsLog.removeLogger(logger);
		app.waitForAppThread();
		app.stop();
		server.stop();
		server1.stop();
		server2.stop();
		speedTestServer.stop();
	}

	/**
	 * 测试 自定义ttl 能力
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testTtlChanger() throws InterruptedException {

		String hostWithShorterTtl = RandomValue.randomHost();
		String hostWithChangerTtl = RandomValue.randomHost();

		CacheTtlChanger changer = Mockito.mock(CacheTtlChanger.class);
		Mockito.when(changer.changeCacheTtl(hostWithShorterTtl, RequestIpType.v4, 2)).thenReturn(1);
		Mockito.when(changer.changeCacheTtl(hostWithChangerTtl, RequestIpType.v4, 1)).thenReturn(2);

		// 重置，然后重新初始化httpdns
		HttpDns.resetInstance();
		new InitConfig.Builder().configCacheTtlChanger(changer).setEnableExpiredIp(false).buildFor(
			app.getAccountId());
		app.start(true);

		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			hostWithShorterTtl, 2);
		server.getResolveHostServer().preSetRequestResponse(hostWithShorterTtl, response, -1);
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips = app.requestResolveHost(hostWithShorterTtl);
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		// 验证服务器收到了请求
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"当没有缓存时，会异步请求服务器", app,
			ResolveHostServer.ResolveHostArg.create(hostWithShorterTtl), server, response, 1,
            true);
		// 再次请求，获取服务器返回的结果
		ips = app.requestResolveHost(hostWithShorterTtl);
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips, response.getIps());

		Thread.sleep(1000);
		// 由于修改了ttl, 过期了，请求ip
		ips = app.requestResolveHost(hostWithShorterTtl);
		UnitTestUtil.assertIpsEmpty("ip过期后，返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"ttl过期后，再次请求会触发网络请求", app,
			ResolveHostServer.ResolveHostArg.create(hostWithShorterTtl), server, response, 1,
            true);
		// 再次请求，获取再次请求服务器返回的结果
		ips = app.requestResolveHost(hostWithShorterTtl);
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips, response.getIps());

		ResolveHostResponse response1 = ResolveHostServer.randomResolveHostResponse(
			hostWithChangerTtl, 1);
		server.getResolveHostServer().preSetRequestResponse(hostWithChangerTtl, response1, -1);
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips1 = app.requestResolveHost(hostWithChangerTtl);
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips1);
		// 验证服务器收到了请求
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"当没有缓存时，会异步请求服务器", app,
			ResolveHostServer.ResolveHostArg.create(hostWithChangerTtl), server, response1, 1,
			true);
		// 再次请求，获取服务器返回的结果
		ips1 = app.requestResolveHost(hostWithChangerTtl);
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips1, response1.getIps());

		Thread.sleep(1000);
		// 由于修改了ttl, 没有过期，请求ip 返回缓存结果
		ips1 = app.requestResolveHost(hostWithChangerTtl);
		// 服务没有收到请求
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		UnitTestUtil.assertIpsEqual("解析域名返回缓存结果", ips1, response1.getIps());
	}

	/**
	 * 主站域名的ip解析缓存 不会因为网络变化而清除
	 */
	@Test
	@Config(shadows = {ShadowNetworkInfo.class})
	public void testCacheWillNotBeCleanWhenNetworkChangeAsIpIsFixed() {

		// 重置，然后重新初始化httpdns, 配置主站域名
		HttpDns.resetInstance();
		ArrayList<String> hosts = new ArrayList<>();
		hosts.add(app.getRequestHost());
		new InitConfig.Builder().configHostWithFixedIp(hosts).buildFor(app.getAccountId());
		app.start(true);

		// 用于和主站域名的效果进行对比
		final String hostWithoutFixedIP = RandomValue.randomHost();

		// 移动网络
		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

		// 先请求一次，产生缓存
		app.requestResolveHost();
		app.requestResolveHost(hostWithoutFixedIP);
		app.waitForAppThread();
		String[] serverResponseIps =
            server.getResolveHostServer().getResponse(app.getRequestHost(),
			1, true).get(0).getIps();
		String[] serverResponseIpsWillChange = server.getResolveHostServer().getResponse(
			hostWithoutFixedIP, 1, true).get(0).getIps();

		// 修改为wifi
		app.changeToNetwork(ConnectivityManager.TYPE_WIFI);

		// 再请求一次，应该使用的是缓存
		UnitTestUtil.assertIpsEqual("再次请求获取的是上次请求的缓存", app.requestResolveHost(),
			serverResponseIps);
		MatcherAssert.assertThat("非主站域名再次请求获取的是不一样的IP",
			app.requestResolveHost(hostWithoutFixedIP),
			Matchers.not(Matchers.arrayContainingInAnyOrder(serverResponseIpsWillChange)));
	}

	/**
	 * 主站域名的ip解析缓存 不会因为网络变化而预解析
	 */
	@Test
	@Config(shadows = {ShadowNetworkInfo.class})
	public void testCacheWillNotBeRefreshWhenNetworkChangeAsIpIsFixed() {

		// 重置，然后重新初始化httpdns, 配置主站域名
		HttpDns.resetInstance();
		ArrayList<String> hosts = new ArrayList<>();
		hosts.add(app.getRequestHost());
		new InitConfig.Builder().configHostWithFixedIp(hosts).buildFor(app.getAccountId());
		app.start(true);
		// 这里设置为网络变化预解析，强化这个配置不影响主站域名
		app.enableResolveAfterNetworkChange(true);

		// 用于和主站域名的效果进行对比
		final String hostWithoutFixedIP = RandomValue.randomHost();

		// 移动网络
		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

		// 先请求一次，产生缓存
		app.requestResolveHost();
		app.requestResolveHost(hostWithoutFixedIP);
		app.waitForAppThread();

		// 修改为wifi
		app.changeToNetwork(ConnectivityManager.TYPE_WIFI);
		MatcherAssert.assertThat("不会触发预解析", server.getBatchResolveHostServer()
			.hasRequestForHost(app.getRequestHost(), RequestIpType.v4, 0, false));
		MatcherAssert.assertThat("非主站域名会触发预解析", server.getBatchResolveHostServer()
			.hasRequestForHost(hostWithoutFixedIP, RequestIpType.v4, 1, false));
	}

	/**
	 * 主站域名的ip解析缓存 默认使用本地缓存
	 */
	@Test
	public void testDiskCacheAsDefaultAsIpIsFixed() {

		// 重置，然后重新初始化httpdns, 配置主站域名
		HttpDns.resetInstance();
		ArrayList<String> hosts = new ArrayList<>();
		hosts.add(app.getRequestHost());
		// 这里配置关闭本地缓存，强化这个配置不影响主站域名
		new InitConfig.Builder().configHostWithFixedIp(hosts).setEnableCacheIp(false).buildFor(
			app.getAccountId());
		app.start(true);

		// 非主站域名用于对比
		final String hostWithoutFixedIP = RandomValue.randomHost();

		// 先请求一次，产生缓存
		app.requestResolveHost();
		app.requestResolveHost(hostWithoutFixedIP);
		app.waitForAppThread();
		String[] serverResponseIps =
            server.getResolveHostServer().getResponse(app.getRequestHost(),
			1, true).get(0).getIps();

		// 重置，重新初始化，触发读取缓存逻辑
		HttpDns.resetInstance();
		new InitConfig.Builder().configHostWithFixedIp(hosts).setEnableCacheIp(false).buildFor(
			app.getAccountId());
		app.start(true);

		// 请求一次，读取缓存
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("主站域名默认开启本地缓存", ips, serverResponseIps);
		UnitTestUtil.assertIpsEmpty("非主站域名没有开启本地缓存",
			app.requestResolveHost(hostWithoutFixedIP));
	}

	/**
	 * 空解析缓存 不会因为网络变化而预解析
	 */
	@Test
	@Config(shadows = {ShadowNetworkInfo.class})
	public void testCacheWillNotBeRefreshWhenNetworkChangeAsEmptyIP() {

		final String hostWithEmptyIP = RandomValue.randomHost();
		server.getResolveHostServer().preSetRequestResponse(hostWithEmptyIP,
			ResolveHostServer.createResponseWithEmptyIp(hostWithEmptyIP, 100), -1);

		// 这里设置为网络变化预解析，强化这个配置不影响主站域名
		app.enableResolveAfterNetworkChange(true);

		// 移动网络
		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

		// 先请求一次，产生缓存
		app.requestResolveHost(hostWithEmptyIP);
		app.waitForAppThread();

		// 修改为wifi
		app.changeToNetwork(ConnectivityManager.TYPE_WIFI);

		MatcherAssert.assertThat("不会触发预解析", server.getBatchResolveHostServer()
			.hasRequestForHost(hostWithEmptyIP, RequestIpType.v4, 0, false));

		// 再请求一次，直接返回的应该是缓存。 这里的目的是强化目前是有缓存的
		app.requestResolveHost(hostWithEmptyIP);
		app.waitForAppThread();
		MatcherAssert.assertThat("服务只接收到第一次请求",
			server.getResolveHostServer().hasRequestForArg(hostWithEmptyIP, 1, true));
	}

	/**
	 * 空解析缓存 强制使用本地缓存
	 */
	@Test
	public void testDiskCacheAsDefaultAsEmptyIP() {

		final String hostWithEmptyIP = RandomValue.randomHost();
		server.getResolveHostServer().preSetRequestResponse(hostWithEmptyIP,
			ResolveHostServer.createResponseWithEmptyIp(hostWithEmptyIP, 100), -1);

		// 显式设置 不开启本地缓存，避免测试干扰
		HttpDns.resetInstance();
		new InitConfig.Builder().setEnableCacheIp(false).buildFor(app.getAccountId());
		app.start(true);

		// 先请求一次，产生缓存
		app.requestResolveHost(hostWithEmptyIP);
		app.waitForAppThread();

		// 重置，重新初始化，触发读取缓存逻辑
		HttpDns.resetInstance();
		new InitConfig.Builder().setEnableCacheIp(false).buildFor(app.getAccountId());
		app.start(true);

		// 请求一次，读取缓存
		app.requestResolveHost(hostWithEmptyIP);
		app.waitForAppThread();
		MatcherAssert.assertThat("服务只接收到第一次请求",
			server.getResolveHostServer().hasRequestForArg(hostWithEmptyIP, 1, true));
	}

	/**
	 * 预解析时，对于v4和v6结果的ttl 独立
	 */
	@Test
	public void testTtlDiffFromType() throws InterruptedException {
		ArrayList<String> hosts = new ArrayList<>();
		String host = RandomValue.randomHost();
		hosts.add(host);
		ArrayList<BatchResolveHostResponse.HostItem> items = new ArrayList<>();
		items.add(
			new BatchResolveHostResponse.HostItem(host, RequestIpType.v4, RandomValue.randomIpv4s(),
                1));
		items.add(
			new BatchResolveHostResponse.HostItem(host, RequestIpType.v6, RandomValue.randomIpv6s(),
				300));
		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hosts, RequestIpType.both),
			new BatchResolveHostResponse(items),
			-1);
		server.getResolveHostServer().preSetRequestResponse(
			host,
			ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(), null, 1, null),
			-1);
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
			ResolveHostServer.createResponse(host, null, RandomValue.randomIpv6s(), 300, null),
			-1);

		// 预解析
		app.preResolveHost(hosts, RequestIpType.both);
		app.waitForAppThread();

		// 等待ttl过期
		Thread.sleep(1000);

		// 请求v4 会触发异步请求
		app.requestResolveHost(host);
		app.waitForAppThread();
		MatcherAssert.assertThat("ttl过期后，异步请求",
			server.getResolveHostServer().hasRequestForArg(host, 1, true));

		// 请求v6，因为没有过期 不会触发异步请求
		app.requestResolveHostForIpv6(host);
		app.waitForAppThread();
		MatcherAssert.assertThat("v6的ttl与v4不同，未过期，不会触发异步请求",
			server.getResolveHostServer()
				.hasRequestForArg(ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
					0, false));
	}

	/**
	 * 预解析会过滤掉空解析域名,
	 * 其实这里本质上是过滤掉了有缓存的域名解析
	 */
	@Test
	public void testResolveFilterEmptyIP() {

		ArrayList<String> v6EmptyHost = new ArrayList<>();
		ArrayList<String> v4EmptyHost = new ArrayList<>();
		ArrayList<String> normalHost = new ArrayList<>();
		ArrayList<String> allHost = new ArrayList<>();

		// 创建不同情况的域名和服务数据
		int count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.both),
				ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(), null, 300, null),
				-1);
			v6EmptyHost.add(host);
			allHost.add(host);
		}

		count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.both),
				ResolveHostServer.createResponse(host, null, RandomValue.randomIpv6s(), 300, null),
				-1);
			v4EmptyHost.add(host);
			allHost.add(host);
		}

		count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.both),
				ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(),
					RandomValue.randomIpv6s(), 300, null),
				-1);
			normalHost.add(host);
			allHost.add(host);
		}

		// 请求所有的域名，产生缓存
		for (String host : allHost) {
			app.requestResolveHost(host, RequestIpType.both);
			app.waitForAppThread();
		}

		// 修改域名的顺序
		allHost = UnitTestUtil.changeArrayListSort(allHost);

		// 重置，重新初始化，清空内存缓存，重新从本地缓存读取
		HttpDns.resetInstance();
		new InitConfig.Builder().setEnableCacheIp(false).buildFor(app.getAccountId());
		app.start(true);

		// 预解析所有的域名
		app.preResolveHost(allHost, RequestIpType.both);
		app.waitForAppThread();

		// 检测预解析是否符合预期
		for (String host : v6EmptyHost) {
			MatcherAssert.assertThat("v6为空的，只会发起v4解析",
				server.getBatchResolveHostServer().hasRequestForHost(host, RequestIpType.v4, 1, false));
		}
		for (String host : v4EmptyHost) {
			MatcherAssert.assertThat("v4为空的，只会发起v6解析",
				server.getBatchResolveHostServer().hasRequestForHost(host, RequestIpType.v6, 1, false));
		}
		for (String host : normalHost) {
			MatcherAssert.assertThat("v4 v6都有的，不过滤", server.getBatchResolveHostServer()
				.hasRequestForHost(host, RequestIpType.both, 1, false));
		}
	}

	/**
	 * 预解析时 过滤掉有效的缓存，仅解析过期的或者不存在的域名
	 */
	@Test
	public void testPreResolveFilterValidCache() {

		ArrayList<String> v4Host = new ArrayList<>();
		ArrayList<String> v6Host = new ArrayList<>();
		ArrayList<String> bothHost = new ArrayList<>();
		ArrayList<String> allHost = new ArrayList<>();

		// 创建不同情况的域名和服务数据
		int count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4),
				ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(), null, 300, null),
				-1);
			v4Host.add(host);
			allHost.add(host);
		}

		count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
				ResolveHostServer.createResponse(host, null, RandomValue.randomIpv6s(), 300, null),
				-1);
			v6Host.add(host);
			allHost.add(host);
		}

		count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.both),
				ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(),
					RandomValue.randomIpv6s(), 300, null),
				-1);
			bothHost.add(host);
			allHost.add(host);
		}

		// 请求所有的域名，产生缓存
		for (String host : v4Host) {
			app.requestResolveHost(host, RequestIpType.v4);
			app.waitForAppThread();
		}
		for (String host : v6Host) {
			app.requestResolveHost(host, RequestIpType.v6);
			app.waitForAppThread();
		}
		for (String host : bothHost) {
			app.requestResolveHost(host, RequestIpType.both);
			app.waitForAppThread();
		}

		// 修改域名的顺序
		allHost = UnitTestUtil.changeArrayListSort(allHost);

		// 预解析所有的域名
		app.preResolveHost(allHost, RequestIpType.both);
		app.waitForAppThread();

		// 检测预解析是否符合预期
		for (String host : v4Host) {
			MatcherAssert.assertThat("v4有效，只会发起v6请求",
				server.getBatchResolveHostServer().hasRequestForHost(host, RequestIpType.v6, 1, false));
		}
		for (String host : v6Host) {
			MatcherAssert.assertThat("v6有效，只会发起v4解析",
				server.getBatchResolveHostServer().hasRequestForHost(host, RequestIpType.v4, 1, false));
		}
		for (String host : bothHost) {
			MatcherAssert.assertThat("v4 v6都有效，不会请求",
				server.getBatchResolveHostServer().hasRequestForHost(host, RequestIpType.both, 0, false)
					&& server.getBatchResolveHostServer()
					.hasRequestForHost(host, RequestIpType.v4, 0, false)
					&& server.getBatchResolveHostServer()
					.hasRequestForHost(host, RequestIpType.v6, 0, false));
		}
	}

	/**
	 * 域名解析时，过滤已有缓存的域名
	 */
	@Test
	public void testResolveFilterValidCache() {

		ArrayList<String> v4Host = new ArrayList<>();
		ArrayList<String> v6Host = new ArrayList<>();
		ArrayList<String> allHost = new ArrayList<>();

		// 创建不同情况的域名和服务数据
		int count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4),
				ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(), null, 300, null),
				-1);
			v4Host.add(host);
			allHost.add(host);
		}

		count = RandomValue.randomInt(5) + 5;
		for (int i = 0; i < count; i++) {
			String host = RandomValue.randomHost();
			server.getResolveHostServer().preSetRequestResponse(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
				ResolveHostServer.createResponse(host, null, RandomValue.randomIpv6s(), 300, null),
				-1);
			v6Host.add(host);
			allHost.add(host);
		}

		// 请求所有的域名，产生缓存
		for (String host : v4Host) {
			app.requestResolveHost(host, RequestIpType.v4);
			app.waitForAppThread();
		}
		for (String host : v6Host) {
			app.requestResolveHost(host, RequestIpType.v6);
			app.waitForAppThread();
		}

		// 修改域名的顺序
		allHost = UnitTestUtil.changeArrayListSort(allHost);

		// 解析所有的域名
		for (String host : allHost) {
			app.requestResolveHost(host, RequestIpType.both);
			app.waitForAppThread();
		}

		// 检测预解析是否符合预期
		for (String host : v4Host) {
			MatcherAssert.assertThat("v4缓存有效，只会发起v6请求", server.getResolveHostServer()
				.hasRequestForArg(ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
					1, true));
		}
		for (String host : v6Host) {
			MatcherAssert.assertThat("v6缓存有效，只会发起v4请求", server.getResolveHostServer()
				.hasRequestForArg(ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4),
					1, true));
		}
	}

	/**
	 * 同步解析过滤已有缓存的域名
	 */
	@Test
	public void testSyncResolveFilterValidCache() throws Throwable {
		UnitTestUtil.testInSubThread(new Runnable() {
			@Override
			public void run() {
				ArrayList<String> v4Host = new ArrayList<>();
				ArrayList<String> v6Host = new ArrayList<>();
				ArrayList<String> allHost = new ArrayList<>();

				// 创建不同情况的域名和服务数据
				int count = RandomValue.randomInt(5) + 5;
				for (int i = 0; i < count; i++) {
					String host = RandomValue.randomHost();
					server.getResolveHostServer().preSetRequestResponse(
						ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4),
						ResolveHostServer.createResponse(host, RandomValue.randomIpv4s(), null,
                            300,
							null),
						-1);
					v4Host.add(host);
					allHost.add(host);
				}

				count = RandomValue.randomInt(5) + 5;
				for (int i = 0; i < count; i++) {
					String host = RandomValue.randomHost();
					server.getResolveHostServer().preSetRequestResponse(
						ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6),
						ResolveHostServer.createResponse(host, null, RandomValue.randomIpv6s(),
                            300,
							null),
						-1);
					v6Host.add(host);
					allHost.add(host);
				}

				// 请求所有的域名，产生缓存
				for (String host : v4Host) {
					app.requestResolveHostSync(host, RequestIpType.v4);
				}
				for (String host : v6Host) {
					app.requestResolveHostSync(host, RequestIpType.v6);
				}

				// 修改域名的顺序
				allHost = UnitTestUtil.changeArrayListSort(allHost);

				// 解析所有的域名
				for (String host : allHost) {
					app.requestResolveHostSync(host, RequestIpType.both);
				}

				// 检测预解析是否符合预期
				for (String host : v4Host) {
					MatcherAssert.assertThat("v4缓存有效，只会发起v6请求",
						server.getResolveHostServer().hasRequestForArg(
							ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v6), 1,
							true));
				}
				for (String host : v6Host) {
					MatcherAssert.assertThat("v6缓存有效，只会发起v4请求",
						server.getResolveHostServer().hasRequestForArg(
							ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4), 1,
							true));
				}
			}
		});
	}

	/**
	 * 测试不需要 重试和切换服务IP 的错误场景
	 */
	@Test
	public void testUnsignedInterfaceDisabled() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_UNSIGNED);
		testErrorWillCreateEmptyCache(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_UNSIGNED);
	}

	@Test
	public void testSignatureExpired() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_SIGNATURE_EXPIRED);
	}

	@Test
	public void testInvalidSignature() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_INVALID_SIGNATURE);
	}

	@Test
	public void testInvalidAccount() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_INVALID_ACCOUNT);
		testErrorWillCreateEmptyCache(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_INVALID_ACCOUNT);
	}

	@Test
	public void testAccountNotExists() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_ACCOUNT_NOT_EXISTS);
		testErrorWillCreateEmptyCache(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_ACCOUNT_NOT_EXISTS);
	}

	@Test
	public void testInvalidDuration() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_INVALID_DURATION);
	}

	@Test
	public void testInvalidHost() {
		testErrorNoRetryNoChangeServerIP(HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_INVALID_HOST);
	}

	@Test
	public void testRetryAndChangeServerIPForOtherError() {
		testErrorRetryChangeServerIP(HttpException.ERROR_CODE_403, "whatever");
	}

	private void testErrorNoRetryNoChangeServerIP(int statusCode, String code) {
		String host = RandomValue.randomHost();
		server.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server1.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server2.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);

		app.setLogger();

		// 请求解析，触发错误处理逻辑
		app.requestResolveHost(host);
		app.waitForAppThread();

		MatcherAssert.assertThat("应该没有重试",
			server.getResolveHostServer().hasRequestForArg(host, 1, true));
		MatcherAssert.assertThat("也没有切换服务重试",
			server1.getResolveHostServer().hasRequestForArg(host, 0, false));

		String anotherHost = RandomValue.randomHost();
		app.requestResolveHost(anotherHost);
		app.waitForAppThread();
		MatcherAssert.assertThat("没有切换服务IP",
			server.getResolveHostServer().hasRequestForArg(anotherHost, 1, true));

		// 日志
		app.hasReceiveLogInLogger(code);
		app.removeLogger();
	}

	private void testErrorRetryChangeServerIP(int statusCode, String code) {
		String host = RandomValue.randomHost();
		server.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server1.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server2.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);

		app.setLogger();

		// 请求解析，触发错误处理逻辑
		app.requestResolveHost(host);
		app.waitForAppThread();

		MatcherAssert.assertThat("因为切换服务，第一个服务节点只请求了一次",
			server.getResolveHostServer().hasRequestForArg(host, 1, true));
		MatcherAssert.assertThat("切换服务重试",
			server1.getResolveHostServer().hasRequestForArg(host, 1, true));

		String anotherHost = RandomValue.randomHost();
		app.requestResolveHost(anotherHost);
		app.waitForAppThread();
		MatcherAssert.assertThat("切换服务IP",
			server2.getResolveHostServer().hasRequestForArg(anotherHost, 1, true));

		// 日志
		app.hasReceiveLogInLogger(code);
		app.removeLogger();
	}

	private String getErrorBody(String code) {
		return "{\"code\":\"" + code + "\"}";
	}

	private void testErrorWillCreateEmptyCache(int statusCode, String code) {
		String host = RandomValue.randomHost();
		server.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server1.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);
		server2.getResolveHostServer().preSetRequestResponse(host, statusCode, getErrorBody(code),
			-1);

		// 请求解析，触发错误处理逻辑
		app.requestResolveHost(host);
		app.waitForAppThread();

		// 清除服务记录
		server.getResolveHostServer().cleanRecord();
		server1.getResolveHostServer().cleanRecord();
		server2.getResolveHostServer().cleanRecord();

		// 再次请求
		String[] ips = app.requestResolveHost(host);
		app.waitForAppThread();
		UnitTestUtil.assertIpsEmpty("生成的应该是空记录", ips);
		MatcherAssert.assertThat("没有服务接收到请求",
			server.getResolveHostServer().hasRequestForArg(host, 0, false)
				&& server1.getResolveHostServer().hasRequestForArg(host, 0, false)
				&& server2.getResolveHostServer().hasRequestForArg(host, 0, false));
	}

}
