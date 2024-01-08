package com.alibaba.sdk.android.httpdns;

import android.Manifest;
import android.net.ConnectivityManager;

import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.resolve.BatchResolveHostResponse;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.ResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.server.BatchResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.server.ServerIpsServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * HTTPDNS end2end test case
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class HttpDnsE2E {

	private final String REGION_DEFAULT = "sg";
	private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(20));
	private BusinessApp app1 = new BusinessApp(RandomValue.randomStringWithFixedLength(20));

	private HttpDnsServer server = new HttpDnsServer();
	private HttpDnsServer server1 = new HttpDnsServer();
	private HttpDnsServer server2 = new HttpDnsServer();

	private HttpDnsServer server3 = new HttpDnsServer();
	private HttpDnsServer server4 = new HttpDnsServer();
	private HttpDnsServer server5 = new HttpDnsServer();
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
		// 这里我们启动6个 服务节点用于测试
		server.start();
		server1.start();
		server2.start();
		server3.start();
		server4.start();
		server5.start();
		ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
		application.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app1.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2},
			null);
		app1.configSpeedTestSever(speedTestServer);
		// 启动两个httpdns实例用于测试
		app.start(true);
		app1.start(true);
	}

	@After
	public void tearDown() {
		HttpDnsLog.removeLogger(logger);
		app.waitForAppThread();
		app1.waitForAppThread();
		app.stop();
		app1.stop();
		server.stop();
		server1.stop();
		server2.stop();
		server3.stop();
		server4.stop();
		server5.stop();
		speedTestServer.stop();
	}

	/**
	 * 解析域名获取ipv4结果
	 */
	@Test
	public void resolveHostToIpv4s() {
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		// 验证服务器收到了请求
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
		String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
		// 再次请求，获取服务器返回的结果
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips, serverResponseIps);
	}

	/**
	 * 启动日志，会在logcat输出日志
	 */
	@Test
	public void enableLogWillPrintLogInLogcat() {
		ShadowLog.clear();
		HttpDnsLog.enable(true);
		doSomethingTriggerLog();
		app.hasReceiveLogInLogcat(true);
	}

	/**
	 * 停止日志，logcat无法获取日志
	 */
	@Test
	public void disableLogWillNotPrintLogInLogcat() {
		ShadowLog.clear();
		HttpDnsLog.enable(false);
		doSomethingTriggerLog();
		app.hasReceiveLogInLogcat(false);
	}

	/**
	 * 设置logger，会在logger中接收到日志
	 */
	@Test
	public void setLoggerWillPrintLogToLogger() {
		app.setLogger();
		doSomethingTriggerLog();
		app.hasReceiveLogInLogger();
		app.removeLogger();
	}

	/**
	 * 进行一些操作，触发日志
	 */
	private void doSomethingTriggerLog() {
		resolveHostToIpv4s();
	}

	/**
	 * 修改region，触发更新服务IP，获取新的服务
	 */
	@Test
	public void changeRegionWillUpdateServerIp() {
		final String defaultRegion = REGION_DEFAULT;
		final String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;

		// 设置不同region对应的服务信息
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		// 修改region
		app.changeRegionTo(otherRegion);
		// 连续请求多次 不影响
		app.changeRegionTo(otherRegion);
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server,
			otherRegion);
		// 请求域名解析
		app.requestResolveHost();
		// 确认是另一个服务接受到请求
		ServerStatusHelper.hasReceiveAppResolveHostRequest(
			"切换region后，新的服务收到域名解析请求", app, server3, 1);

		// 再把region切换回来
		app.changeRegionTo(defaultRegion);
		ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server3,
			defaultRegion);
		app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequest(
			"切回region后，原来的服务收到域名解析请求", app, server, 1);

	}

	/**
	 * 预设region更新请求
	 * defaultRegion 对应 server 0 1 2
	 * anotherRegion 对应 server 3 4 5
	 *
	 * @param defaultRegion
	 * @param anotherRegion
	 */
	private void prepareUpdateServerResponse(String defaultRegion, String anotherRegion) {
		prepareUpdateServerResponseForGroup1(anotherRegion);
		prepareUpdateServerResponseForGroup2(defaultRegion);
	}

	private void prepareUpdateServerResponseForGroup2(String defaultRegion) {
		// 给 group2（服务3、4、5） 设置 defaultRegion的服务IP是 服务 0 1 2
		String anotherUpdateServerResponse = ServerIpsServer.createUpdateServerResponse(
			new String[] {server.getServerIp(), server1.getServerIp(), server2.getServerIp()},
			RandomValue.randomIpv6s(),
			new int[] {server.getPort(), server1.getPort(), server2.getPort()},
			RandomValue.randomPorts());
		server3.getServerIpsServer().preSetRequestResponse(defaultRegion, 200,
			anotherUpdateServerResponse, -1);
		server4.getServerIpsServer().preSetRequestResponse(defaultRegion, 200,
			anotherUpdateServerResponse, -1);
		server5.getServerIpsServer().preSetRequestResponse(defaultRegion, 200,
			anotherUpdateServerResponse, -1);
	}

	private void prepareUpdateServerResponseForGroup1(String anotherRegion) {
		// 给 group1（服务0、1、2） 设置 anotherRegion的服务IP是 服务 3 4 5
		String updateServerResponse = ServerIpsServer.createUpdateServerResponse(
			new String[] {server3.getServerIp(), server4.getServerIp(), server5.getServerIp()},
			RandomValue.randomIpv6s(),
			new int[] {server3.getPort(), server4.getPort(), server5.getPort()},
			RandomValue.randomPorts());
		server.getServerIpsServer().preSetRequestResponse(anotherRegion, 200, updateServerResponse,
			-1);
		server1.getServerIpsServer().preSetRequestResponse(anotherRegion, 200,
			updateServerResponse,
			-1);
		server2.getServerIpsServer().preSetRequestResponse(anotherRegion, 200,
			updateServerResponse,
			-1);
	}

	/**
	 * 测试ip probe功能，
	 * 解析域名之后，对返回的ip进行测试，按照速度快慢排序
	 */
	@Test
	public void sortIpArrayWithSpeedAfterResolveHost() {

		speedTestServer.watch(server);

		// 配置IP优先
		app.enableIPRanking();

		// 请求数据触发IP优选
		app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequest("服务接受到域名解析请求", app, server,
			1);

		// 判断返回的结果是优选的结果
		String[] ips = app.requestResolveHost();
		String[] sortedIps = speedTestServer.getSortedIpsFor(app.getRequestHost());

		UnitTestUtil.assertIpsEqual("设置ip优选后，返回的ip是优选之后的结果", ips, sortedIps);
	}

	/**
	 * 正常模式下，域名解析失败，会自动重试一次
	 */
	@Test
	public void resolveHostRequestWillRetryOnceWhenFailed() {
		// 预置第一次失败，第二次成功
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), 400,
			"whatever",
			1);
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server1.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

		// 请求
		app.requestResolveHost();
		// 判断服务器是否收到两次请求
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(app, server, 400,
			"whatever");
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"正常模式域名解析失败会重试一次", app, server1, response);

		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("重试如果请求成功，可以正常获取到解析结果", ips,
			response.getIps());
	}

	/**
	 * 设置超时
	 */
	@Test
	public void configReqeustTimeout() {
		// 预设请求超时
		server.getResolveHostServer().preSetRequestTimeout(app.getRequestHost(), -1);

		// 设置超时时间
		int timeout = 1000;
		app.setTimeout(timeout);

		// 请求 并计时
		long start = System.currentTimeMillis();
		app.requestResolveHost();
		// 确实是否接受到请求，并超时
		ServerStatusHelper.hasReceiveAppResolveHostRequestButTimeout(app, server);
		long costTime = System.currentTimeMillis() - start;

		// 3.05 是个经验数据，可以考虑调整。影响因素主要有重试次数和线程切换
		assertThat("requst timeout " + costTime, costTime < timeout * 3.05);
	}

	/**
	 * 解析域名时，如果服务不可用，会切换服务IP
	 */
	@Test
	public void interpretHostRequestWillShiftServerIpWhenCurrentServerNotAvailable() {
		// 预设服务0超时
		server.getResolveHostServer().preSetRequestTimeout(app.getRequestHost(), -1);
		// 预设服务1正常返回
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server1.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

		// 设置超时，并请求
		app.setTimeout(1000);
		app.requestResolveHost();
		// 确认服务0 接受到请求，超时
		ServerStatusHelper.hasReceiveAppResolveHostRequestButTimeout(app, server);
		// 确认服务1 正常返回
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("服务不可用时，会切换服务IP",
			app, server1, response);

		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips,
			response.getIps());

		// 确认后续请求都是实用切换后的服务
		ServerStatusHelper.requestResolveAnotherHost("切换服务IP后，后续请求都使用此服务", app,
			server1);
	}

	/**
	 * 解析域名时，如果服务降级，会切换服务IP
	 */
	@Test
	public void interpretHostRequestWillShiftServerIpWhenCurrentServerDegrade() {
		// 预设服务0降级
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), 1);
		// 预设服务1正常返回
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server1.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

		// 设置超时，并请求
		app.setTimeout(1000);
		app.requestResolveHost();
		// 确认服务0 接受到请求，降级
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithDegrade(app, server);
		// 确认服务1 正常返回
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("服务降级时会切换服务IP",
			app, server1, response);

		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips,
			response.getIps());

		// 确认后续请求都是实用切换后的服务
		ServerStatusHelper.requestResolveAnotherHost("切换服务IP后，后续请求都使用此服务", app,
			server1);
	}

	/**
	 * 当现有的服务IP都失败时，更新服务IP
	 */
	@Test
	public void updateServerIpWhenAllServerIpFail() {
		// 设置更新服务IP数据
		prepareUpdateServerResponse(REGION_DEFAULT, REGION_DEFAULT);
		// 前三个server设置为不可用
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);

		// 请求 切换服务IP，每次请求 重试1次，请求两次，服务IP 换一轮，触发更新服务IP
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();

		// 检查服务IP是否已经更新
		ServerStatusHelper.requestResolveAnotherHost("更新服务IP后，使用新服务解析域名", app,
			server3);
	}

	/**
	 * 当切换两个服务IP解析域名都是超时或者降级时，进入嗅探模式
	 */
	@Test
	public void whenServerIpsFailTwiceEnterSniffMode() {
		// 前两个server设置为不可用
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
		// 设置server2 一次请求失败，用于嗅探模式请求
		ServerStatusHelper.setError(server2, app.getRequestHost(), 400, "whatever", 1);

		// 请求 切换服务IP，重试1次，一共失败两次，触发sniff模式
		app.requestResolveHost();
		app.waitForAppThread();
		// 嗅探模式下请求一次
		app.requestResolveHost();
		// 服务接受到一次请求，返回失败
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(app, server2, 400,
			"whatever", 1, true);
		// 没有接收到第二次请求，即没有重试
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("嗅探模式没有重试", app, server2);
		// 再次请求
		app.requestResolveHost(app.getRequestHost());
		// 没有接收到第三次请求，即30s内不能重复请求
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("嗅探模式30s内不能再次请求", app,
			server2);
	}

	/**
	 * 嗅探模式下，请求成功，会退出嗅探模式
	 */
	@Test
	public void whenResolveHostSuccessExitSniffMode() {
		// 前两个server设置为不可用
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
		// 设置server2 用于嗅探模式请求

		// 请求 切换服务IP，重试1次，一共失败两次，触发sniff模式
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();
		// 嗅探模式下请求一次，恢复正常模式
		ServerStatusHelper.requestResolveAnotherHost("嗅探模式下，正常请求", app, server2);

		// 清除服务的记录
		server.getResolveHostServer().cleanRecord();
		server1.getResolveHostServer().cleanRecord();
		server2.getResolveHostServer().cleanRecord();

		// 设置一次失败
		server2.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), 400,
			"whatever", 1);
		app.requestResolveHost(app.getRequestHost());
		// 一次失败，一次正常 两次
		ServerStatusHelper.hasReceiveAppResolveHostRequest("恢复到正常模式后，请求一次。", app,
			server2, 1);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("失败会切换服务，重试一次。", app,
			server, 1);
	}

	/**
	 * 更新服务IP有时间间隔限制
	 */
	@Test
	public void setRegionHasTimeInterval() {
		String defaultRegion = REGION_DEFAULT;
		String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;

		prepareUpdateServerResponse(defaultRegion, otherRegion);

		// 切换到hk
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server,
			otherRegion, true);
		// 切回default
		app.changeRegionTo(defaultRegion);
		ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server3,
			defaultRegion, true);

		// 再切换到hk，因为请求时间间隔太小，不会触发请求
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP请求没有进行时间间隔限制", app,
			server, otherRegion);

		// 再切换回default，因为请求时间间隔太小，不会触发请求
		app.changeRegionTo(defaultRegion);
		ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP请求没有进行时间间隔限制", app,
			server, defaultRegion);

		// 缩短请求间隔
		app.setUpdateServerTimeInterval(1000);
		try {
			Thread.sleep(1001);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 切换到hk
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasReceiveRegionChange("更新服务IP请求超过时间间隔才允许请求", app,
			server, otherRegion);
		// 切回default
		app.changeRegionTo(defaultRegion);
		ServerStatusHelper.hasReceiveRegionChange("更新服务IP请求超过时间间隔才允许请求", app,
			server3, defaultRegion);
	}

	/**
	 * 更新服务IP有时间间隔
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void updateServerIpsHasTimeInterval() throws InterruptedException {

		// 设置更新服务IP数据
		prepareUpdateServerResponse(REGION_DEFAULT, REGION_DEFAULT);
		//设置所有的服务为不可用
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server3, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server4, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server5, app.getRequestHost(), -1);

		// 请求 切换服务IP，每次请求 重试1次，一共请求两次，进入嗅探模式
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();
		// 请求一次， 切换服务IP，触发服务IP更新
		app.requestResolveHost(app.getRequestHost());
		// 检查更新服务IP请求是否触发
		ServerStatusHelper.hasReceiveRegionChange("服务IP切换一遍后，触发服务IP更新", app, server,
			REGION_DEFAULT, true);
		// 更新之后，退出嗅探模式， 再来一次
		// 请求 切换服务IP，每次请求 重试1次，一共请求两次，进入嗅探模式
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();
		// 请求一次， 切换服务IP，触发服务IP更新
		app.requestResolveHost(app.getRequestHost());
		// 因为间隔过小，不会请求服务器
		ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP没有设置时间间隔", app, server,
			REGION_DEFAULT);
		ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP没有设置时间间隔", app, server3,
			REGION_DEFAULT);

		// 缩短时间间隔
		app.setUpdateServerTimeInterval(1000);
		// 缩短嗅探的时间间隔
		app.setSniffTimeInterval(500);
		// 嗅探模式下，连续请求三次，触发服务IP更新
		Thread.sleep(500);
		app.requestResolveHost(app.getRequestHost());
		Thread.sleep(500);
		app.requestResolveHost(app.getRequestHost());
		Thread.sleep(500);
		app.requestResolveHost(app.getRequestHost());
		// 确认服务IP请求触发
		ServerStatusHelper.hasReceiveRegionChange(
			"更新服务IP超过时间间隔才能请求,如果在嗅探模式下，也需要缩短嗅探的时间间隔", app,
			server3, REGION_DEFAULT);
	}

	/**
	 * 更新服务IP请求失败，会切换服务尝试
	 */
	@Test
	public void updateServerFailWhenRetryAnotherServer() {

		String hkRegion = Constants.REGION_HK;
		String updateServerResponse = ServerIpsServer.createUpdateServerResponse(
			new String[] {server3.getServerIp(), server4.getServerIp(), server5.getServerIp()},
			RandomValue.randomIpv6s(),
			new int[] {server3.getPort(), server4.getPort(), server5.getPort()},
			RandomValue.randomPorts());
		// 第一个服务失败
		server.getServerIpsServer().preSetRequestResponse(hkRegion, 400, "whatever", 1);
		server1.getServerIpsServer().preSetRequestResponse(hkRegion, 200, updateServerResponse,
			-1);
		server2.getServerIpsServer().preSetRequestResponse(hkRegion, 200, updateServerResponse,
			-1);

		// 触发服务IP更新，第一次失败，使用第二个服务
		app.changeRegionTo(hkRegion);
		ServerStatusHelper.hasReceiveRegionChange("服务收到更新请求", app, server, hkRegion);
		ServerStatusHelper.hasReceiveRegionChange("更新服务IP时，失败了但是没有遍历尝试其它服务",
			app, server1, hkRegion);
		ServerStatusHelper.hasNotReceiveRegionChange(
			"更新服务IP时，一旦有一个成功，就不会尝试其它服务了", app, server2, hkRegion);
	}

	/**
	 * 当前的服务更新服务IP都失败了，就使用初始服务IP尝试
	 */
	@Test
	public void updateServerAllFailWhenRetryInitServer() {

		String otherRegion = Constants.REGION_HK == REGION_DEFAULT ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		String defaultRegion = REGION_DEFAULT;
		prepareUpdateServerResponseForGroup1(otherRegion);

		// 设置服务不可用
		server3.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);
		server4.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);
		server5.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);

		// 先更新一次服务IP
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasReceiveRegionChange("服务收到更新服务IP请求", app, server,
			otherRegion);

		// 再触发一次服务IP更新，此时全部失败，会切换会初始服务请求
		app.changeRegionTo(defaultRegion);
		ServerStatusHelper.hasReceiveRegionChange("更新服务IP都失败时，没有切换回初始化服务IP尝试",
			app, server, defaultRegion);
	}

	/**
	 * 不同的account使用不同的实例，相互之间不影响
	 */
	@Test
	public void differentAcountUseDifferentInstance() {
		// 确认一般的域名解析请求都正常
		ServerStatusHelper.requestResolveAnotherHost("应用0域名解析服务正常", app, server);
		ServerStatusHelper.requestResolveAnotherHost("应用1域名解析服务正常", app1, server);

		String defaultRegion = REGION_DEFAULT;
		String otherRegion = Constants.REGION_HK == REGION_DEFAULT ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		// 应用0切换到 hk
		app.changeRegionTo(otherRegion);
		ServerStatusHelper.hasReceiveRegionChange("应用0切换到 其它region", app, server,
			otherRegion);
		// 应用1正常使用
		ServerStatusHelper.requestResolveAnotherHost("应用1不受应用0切region影响", app1, server);

		ServerStatusHelper.degradeServer(server, app1.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app1.getRequestHost(), -1);
		ServerStatusHelper.setError(server2, app1.getRequestHost(), 400, "whatever", -1);
		// 请求，服务降级，进入嗅探模式
		app1.requestResolveHost(app1.getRequestHost());
		app1.waitForAppThread();
		// 请求失败, 短时间不能再次嗅探请求
		app1.requestResolveHost(app1.getRequestHost());
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(app1, server2, 400,
			"whatever", 1, true);
		app1.requestResolveHost(app1.getRequestHost());
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("短时间内无法再次嗅探请求", app1,
			server2);

		ServerStatusHelper.requestResolveAnotherHost("应用0不受应用1嗅探状态影响", app, server3);

		app1.changeRegionTo(otherRegion);
		app1.waitForAppThread();

		String sameHost = RandomValue.randomHost();
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(sameHost);
		ResolveHostResponse response1 =
			ResolveHostServer.randomResolveHostResponse(sameHost);
		server3.getResolveHostServer().preSetRequestResponse(sameHost, response, 1);
		server3.getResolveHostServer().preSetRequestResponse(sameHost, response1, 1);

		app.requestResolveHost(sameHost);
		app.waitForAppThread();
		assertThat("应用0获取自己的结果", server3.getResolveHostServer()
			.hasRequestForArgWithResult(sameHost, response, 1, true));
		app1.requestResolveHost(sameHost);
		app1.waitForAppThread();
		assertThat("应用1获取自己的结果", server3.getResolveHostServer()
			.hasRequestForArgWithResult(sameHost, response1, 1, true));
		assertThat("应用0、应用1结果不干扰", app.requestResolveHost(sameHost),
			Matchers.not(Matchers.arrayContaining(app1.requestResolveHost(sameHost))));
	}

	/**
	 * 相同的账号实用相同的实例
	 */
	@Test
	public void sameAccountUseSameInstance() {
		app.checkSameInstanceForSameAcount();
	}

	/**
	 * ipv6支持测试
	 */
	@Test
	public void supportIpv6Resolve() {
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v6),
			response, -1);

		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ipv6s = app.requestResolveHostForIpv6();
		UnitTestUtil.assertIpsEmpty("解析域名，没有缓存时，返回空", ipv6s);
		// 验证服务器收到了请求
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"服务应该收到ipv6域名解析请求", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v6),
			server, response, 1, true);
		// 再次请求，获取服务器返回的结果
		ipv6s = app.requestResolveHostForIpv6();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("有缓存，不会请求服务器", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v6),
			server);
		UnitTestUtil.assertIpsEqual("解析结果是服务返回的值", ipv6s, response.getIpsV6());
	}

	/**
	 * 测试对SDNS的支持
	 */
	@Test
	public void testSDNS() {
		HashMap<String, String> extras = new HashMap<>();
		extras.put("key1", "value1");
		extras.put("key2", "value3");
		String cacheKey = "sdns1";

		ResolveHostResponse normalResponse = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), normalResponse,
			-1);
		ResolveHostResponse sdnsResponse = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v4,
				extras), sdnsResponse, -1);

		HTTPDNSResult result = app.requestSDNSResolveHost(extras, cacheKey);
		UnitTestUtil.assertIpsEmpty("和其它域名解析一样，sdns解析第一次没有缓存时，返回空",
			result.getIps());
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("服务器应该接收到sdns请求",
			app, ResolveHostServer.ResolveHostArg.create(app.getRequestHost(),
				RequestIpType.v4,
				extras), server, sdnsResponse, 1, true);
		result = app.requestSDNSResolveHost(extras, cacheKey);
		UnitTestUtil.assertIpsEqual("sdns解析结果和预期值一致", result.getIps(),
			sdnsResponse.getIps());

		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("解析时没有缓存，返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("一般解析和sdns解析互不干扰",
			app, server, normalResponse);
		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("一般解析结果和预期一致", ips, normalResponse.getIps());
	}

	/**
	 * 测试SDNS全局参数
	 */
	@Test
	public void testGlobalParamsSDNS() {

		HashMap<String, String> globalParams = new HashMap<>();
		globalParams.put("g1", "v1");
		globalParams.put("g2", "v2");
		String globalCacheKey = "gsdns1";

		HashMap<String, String> extras = new HashMap<>();
		extras.put("key1", "value1");
		extras.put("key2", "value3");
		String cacheKey = "sdns";
		String cacheKey1 = "sdns1";
		String cacheKey2 = "sdns2";

		// 一般sdns结果
		ResolveHostResponse sdnsResponse = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v4,
				extras), sdnsResponse, -1);

		// 仅global参数
		ResolveHostResponse gsdnsResponse = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v4,
				globalParams), gsdnsResponse, -1);

		// global参数 + 一般参数
		HashMap<String, String> all = new HashMap();
		all.putAll(globalParams);
		all.putAll(extras);
		ResolveHostResponse sdnsResponse1 = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v4,
				all), sdnsResponse1, -1);

		app.requestSDNSResolveHost(extras, cacheKey);
		app.waitForAppThread();
		HTTPDNSResult result = app.requestSDNSResolveHost(extras, cacheKey);
		UnitTestUtil.assertIpsEqual("sdns解析结果应该和预期值一致", result.getIps(),
			sdnsResponse.getIps());

		app.setGlobalParams(globalParams);
		app.requestSDNSResolveHost(null, globalCacheKey);
		app.waitForAppThread();
		HTTPDNSResult gResult = app.requestSDNSResolveHost(null, globalCacheKey);
		UnitTestUtil.assertIpsEqual("仅global参数，sdns解析结果和预期值一致", gResult.getIps(),
			gsdnsResponse.getIps());

		app.requestSDNSResolveHost(extras, cacheKey1);
		app.waitForAppThread();
		HTTPDNSResult result1 = app.requestSDNSResolveHost(extras, cacheKey1);
		UnitTestUtil.assertIpsEqual("global参数+定制参数，sdns解析结果和预期值一致",
			result1.getIps(), sdnsResponse1.getIps());

		app.cleanGlobalParams();
		app.requestSDNSResolveHost(extras, cacheKey2);
		app.waitForAppThread();
		HTTPDNSResult result2 = app.requestSDNSResolveHost(extras, cacheKey2);
		UnitTestUtil.assertIpsEqual("清楚global参数后，sdns解析结果和仅定制参数的解析结果一致",
			result2.getIps(), sdnsResponse.getIps());
	}

	/**
	 * 测试SDNS解析ipv6
	 */
	@Test
	public void testSDNSForIpv6() {
		HashMap<String, String> extras = new HashMap<>();
		extras.put("key1", "value1");
		extras.put("key2", "value3");
		String cacheKey = "sdns1";

		ResolveHostResponse sdnsResponse = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost(), RequestIpType.v6,
				extras), sdnsResponse, -1);

		HTTPDNSResult result = app.requestSDNSResolveHostForIpv6(extras, cacheKey);
		UnitTestUtil.assertIpsEmpty("和其它域名解析一样，sdns解析第一次没有缓存时，返回空",
			result.getIps());
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("服务器应该接收到sdns请求",
			app, ResolveHostServer.ResolveHostArg.create(app.getRequestHost(),
				RequestIpType.v6,
				extras), server, sdnsResponse, 1, true);
		result = app.requestSDNSResolveHostForIpv6(extras, cacheKey);
		UnitTestUtil.assertIpsEqual("sdns解析结果和预期值一致", result.getIpv6s(),
			sdnsResponse.getIpsV6());
	}

	/**
	 * 预解析ipv4
	 */
	@Test
	public void preResolveHostForIpv4() {
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		ArrayList<String> hostList = new ArrayList<>();
		hostList.add(host1);
		hostList.add(host2);
		hostList.add(host3);

		BatchResolveHostResponse response = BatchResolveHostServer.randomResolveHostResponse(hostList,
			RequestIpType.v4);

		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hostList, RequestIpType.v4), response, 1);

		app.preResolveHost(hostList, RequestIpType.v4);
		app.waitForAppThread();

		String[] ips1 = app.requestResolveHost(host1);
		String[] ips2 = app.requestResolveHost(host2);
		String[] ips3 = app.requestResolveHost(host3);
		UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips1,
			response.getHostItem(host1, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips2,
			response.getHostItem(host2, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips3,
			response.getHostItem(host3, RequestIpType.v4).getIps());
	}

	/**
	 * 预解析ipv6
	 */
	@Test
	public void preResolveHostForIpv6() {
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		ArrayList<String> hostList = new ArrayList<>();
		hostList.add(host1);
		hostList.add(host2);
		hostList.add(host3);

		BatchResolveHostResponse response = BatchResolveHostServer.randomResolveHostResponse(hostList,
			RequestIpType.v6);

		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer. BatchResolveRequestArg.create(hostList, RequestIpType.v6), response, 1);

		app.preResolveHost(hostList, RequestIpType.v6);
		app.waitForAppThread();


		String[] ips1 = app.requestResolveHostForIpv6(host1);
		String[] ips2 = app.requestResolveHostForIpv6(host2);
		String[] ips3 = app.requestResolveHostForIpv6(host3);
		UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips1,
			response.getHostItem(host1, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips2,
			response.getHostItem(host2, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips3,
			response.getHostItem(host3, RequestIpType.v6).getIps());
	}

	/**
	 * 预解析 4 6
	 */
	@Test
	public void preResolveHost() {
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		ArrayList<String> hostList = new ArrayList<>();
		hostList.add(host1);
		hostList.add(host2);
		hostList.add(host3);

		BatchResolveHostResponse response = BatchResolveHostServer.randomResolveHostResponse(hostList,
			RequestIpType.both);

		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hostList, RequestIpType.both), response, 1);

		app.preResolveHost(hostList, RequestIpType.both);
		app.waitForAppThread();

		String[] ips1 = app.requestResolveHost(host1);
		String[] ips2 = app.requestResolveHost(host2);
		String[] ips3 = app.requestResolveHost(host3);
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips1,
			response.getHostItem(host1, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips2,
			response.getHostItem(host2, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips3,
			response.getHostItem(host3, RequestIpType.v4).getIps());

		String[] ips4 = app.requestResolveHostForIpv6(host1);
		String[] ips5 = app.requestResolveHostForIpv6(host2);
		String[] ips6 = app.requestResolveHostForIpv6(host3);
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips4,
			response.getHostItem(host1, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips5,
			response.getHostItem(host2, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips6,
			response.getHostItem(host3, RequestIpType.v6).getIps());
	}

	/**
	 * 当预解析域名超过5个场景
	 */
	@Test
	public void preResolveHostMoreThan5() {
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		String host4 = RandomValue.randomHost();
		String host5 = RandomValue.randomHost();
		String host6 = RandomValue.randomHost();
		String host7 = RandomValue.randomHost();
		ArrayList<String> hostList1 = new ArrayList<>();
		hostList1.add(host1);
		hostList1.add(host2);
		hostList1.add(host3);
		hostList1.add(host4);
		hostList1.add(host5);

		ArrayList<String> hostList2 = new ArrayList<>();
		hostList2.add(host6);
		hostList2.add(host7);

		BatchResolveHostResponse response1 = BatchResolveHostServer.randomResolveHostResponse(hostList1,
			RequestIpType.both);
		BatchResolveHostResponse response2 = BatchResolveHostServer.randomResolveHostResponse(hostList2,
			RequestIpType.both);

		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hostList1, RequestIpType.both), response1,
			1);
		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hostList2, RequestIpType.both), response2,
			1);

		ArrayList<String> hostList = new ArrayList<>();
		hostList.addAll(hostList1);
		hostList.addAll(hostList2);
		app.preResolveHost(hostList, RequestIpType.both);
		app.waitForAppThread();

		String[] ips1 = app.requestResolveHost(host1);
		String[] ips2 = app.requestResolveHost(host2);
		String[] ips3 = app.requestResolveHost(host3);
		String[] ips4 = app.requestResolveHost(host4);
		String[] ips5 = app.requestResolveHost(host5);
		String[] ips6 = app.requestResolveHost(host6);
		String[] ips7 = app.requestResolveHost(host7);
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips1,
			response1.getHostItem(host1, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips2,
			response1.getHostItem(host2, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips3,
			response1.getHostItem(host3, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips4,
			response1.getHostItem(host4, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips5,
			response1.getHostItem(host5, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ips6,
			response2.getHostItem(host6, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ips7,
			response2.getHostItem(host7, RequestIpType.v4).getIps());

		String[] ipv6s1 = app.requestResolveHostForIpv6(host1);
		String[] ipv6s2 = app.requestResolveHostForIpv6(host2);
		String[] ipv6s3 = app.requestResolveHostForIpv6(host3);
		String[] ipv6s4 = app.requestResolveHostForIpv6(host4);
		String[] ipv6s5 = app.requestResolveHostForIpv6(host5);
		String[] ipv6s6 = app.requestResolveHostForIpv6(host6);
		String[] ipv6s7 = app.requestResolveHostForIpv6(host7);
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s1,
			response1.getHostItem(host1, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s2,
			response1.getHostItem(host2, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s3,
			response1.getHostItem(host3, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s4,
			response1.getHostItem(host4, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s5,
			response1.getHostItem(host5, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ipv6s6,
			response2.getHostItem(host6, RequestIpType.v6).getIps());
		UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ipv6s7,
			response2.getHostItem(host7, RequestIpType.v6).getIps());
	}

	/**
	 * 测试 ttl 有效性
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testIpTtl() throws InterruptedException {
		app.enableExpiredIp(false);
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost(), 1);
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, -1);
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		// 验证服务器收到了请求
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"当没有缓存时，会异步请求服务器", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost()), server, response, 1,
			true);
		// 再次请求，获取服务器返回的结果
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);

		Thread.sleep(1000);
		// ttl 过期后请求ip
		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("ip过期后，返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"ttl过期后，再次请求会触发网络请求", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost()), server, response, 1,
			true);
		// 再次请求，获取再次请求服务器返回的结果
		ips = app.requestResolveHost();
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);
	}

	/**
	 * 测试允许返回过期IP功能
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testEnableExpiredIp() throws InterruptedException {
		app.enableExpiredIp(true);
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost(), 1);
		ResolveHostResponse response1 = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response1, -1);
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		app.waitForAppThread();
		// 再次请求，获取服务器返回的结果
		app.requestResolveHost();

		Thread.sleep(1000);
		// ttl 过期后请求ip
		ips = app.requestResolveHost();
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("启用过期IP，请求时域名过期，仍会返回过期IP", response.getIps(),
			ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"ttl过期后，再次请求会触发网络请求", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost()), server, response1
			, 1,
			true);
		// 再次请求，获取再次请求服务器返回的结果
		ips = app.requestResolveHost();
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response1.getIps(), ips);
	}

	/**
	 * 当前服务节点的状态会缓存，比如当前正使用哪个服务节点
	 */
	@Test
	public void testServerCache() {
		// 先通过请求失败，切换一次服务IP
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), 1);
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server1.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
		app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithDegrade(app, server);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("服务降级时会切换服务IP",
			app, server1, response);
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips,
			response.getIps());

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);

		// 确认后续请求都是使用切换后的服务
		ServerStatusHelper.requestResolveAnotherHost("读取缓存应该是直接使用切换后的服务", app,
			server1);
	}

	/**
	 * 测试 当前服务节点不是初始服务节点的 状态缓存
	 */
	@Test
	public void testServerCacheWhenServerIsNotInitServer() {
		//  先通过请求失败，切换服务IP
		prepareUpdateServerResponse(REGION_DEFAULT, REGION_DEFAULT);
		// 前三个server设置为不可用
		ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
		ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);

		// 请求 切换服务IP，每次请求 重试1次，请求两次，服务IP 换一轮，触发更新服务IP
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();
		app.requestResolveHost(app.getRequestHost());
		app.waitForAppThread();

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);

		// 检查服务IP是否已经更新
		ServerStatusHelper.requestResolveAnotherHost("更新服务IP后，使用新服务解析域名", app,
			server3);
	}

	/**
	 * 默认未开启IP缓存，下次开启IP缓存，也读取不到数据
	 */
	@Test
	public void testCacheControll() {
		// 先发起一些请求，因为没有开启缓存，所以不会缓存
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
		String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

		// 重置实例
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("之前没有缓存，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
	}

	/**
	 * 开启缓存的情况下， IP会缓存到本地
	 * 下次读取时 如果 标记clean，会在读取缓存后，删除缓存
	 */
	@Test
	public void testCacheClean() {
		app.enableCache(false);
		// 先发起一些请求，缓存一些Ip结果
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
		String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(true);

		// 此时返回缓存，然后由于是数据库读取的，触发一次解析
		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("解析域名返回缓存结果", serverResponseIps, ips);

		app.waitForAppThread();
		// 由于从数据库读取的结果会触发一次解析更新，所以此处我们在此清除数据库缓存
		app.enableCache(true);

		server.getResolveHostServer().cleanRecord();

		// 重置实例，
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("上次读取缓存时把缓存清除了，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
	}

	/**
	 * 测试 IP 缓存
	 */
	@Test
	public void testIpCache() {
		app.enableCache(false);
		// 先发起一些请求，缓存一些Ip结果
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("当没有缓存时，会异步请求服务器", app,
			server, 1);
		String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("解析域名返回缓存结果", serverResponseIps, ips);
	}

	/**
	 * 1. 当开启IP缓存功能时，从缓存读取的IP，即使不允许返回过期IP，也会返回
	 * 2. 当IP更新后，不再认为是从本地缓存读取，过期了，就返回空
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testIpCacheWhenExpired() throws InterruptedException {
		app.enableCache(false);
		app.enableExpiredIp(false);
		// 先发起一些请求，缓存一些Ip结果
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost(), 1);
		ResolveHostResponse response1 = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost(), 1);
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response1, -1);
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"当没有缓存时，会异步请求服务器", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost()), server, response, 1,
			true);
		ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("当有缓存时，不会请求服务器", app,
			server);
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		Thread.sleep(1000);

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);
		app.enableExpiredIp(false);

		ips = app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"本地缓存过期时，会触发网络请求", app, server, response1);
		UnitTestUtil.assertIpsEqual("本地缓存即使过期也会返回ip", ips, response.getIps());

		Thread.sleep(1000);

		ips = app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult("ttl过期，会触发网络请求",
			app, server, response1);
		UnitTestUtil.assertIpsEmpty("后续更新后，不会判定为从本地缓存读取，应该返回空", ips);
	}

	/**
	 * 测试 域名解析拦截 接口
	 */
	@Test
	public void testHostFilter() {
		app.setFilter();

		app.requestResolveHost();
		app.waitForAppThread();
		String[] ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("因为域名过滤了,不会请求服务器",
			app, server);
		UnitTestUtil.assertIpsEmpty("因为域名过滤了解析一直为空", ips);

		ServerStatusHelper.requestResolveAnotherHost("其它域名不受过滤影响", app, server);
	}

	/**
	 * 测试 域名解析拦截 在 预解析上也生效
	 */
	@Test
	public void testHostFilterForResolve() {
		app.setFilter();

		String anotherHost = RandomValue.randomHost();
		ArrayList<String> list = new ArrayList<>();
		list.add(app.getRequestHost());
		list.add(anotherHost);
		app.preResolveHost(list, RequestIpType.v4);
		app.waitForAppThread();
		String[] ips = app.requestResolveHost();
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest("因为域名过滤了,不会请求服务器",
			app, server);
		UnitTestUtil.assertIpsEmpty("因为域名过滤了解析一直为空", ips);

		list.remove(app.getRequestHost());
	}

	/**
	 * 测试 加签能力
	 */
	@Test
	public void testAuthSign() {
		String account = RandomValue.randomStringWithFixedLength(20);
		String secret = server.createSecretFor(account);
		BusinessApp app2 = new BusinessApp(account, secret);
		app2.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2},
			null);
		app2.configSpeedTestSever(speedTestServer);
		app2.start(true);

		String host = RandomValue.randomHost();
		app2.requestResolveHost(host);
		app2.waitForAppThread();

		ArrayList<String> params = new ArrayList<>();
		params.add("s");
		params.add("t");
		params.add("host");
		MatcherAssert.assertThat("请求服务器时，发出的是带签名的请求",
			server.getResolveHostServer().hasRequestForArgWithParams(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4), params, 1,
				false));
	}

	/**
	 * 测试 签名失效的场景
	 * 测试 校正时间能力
	 */
	@Test
	public void testAuthSignValid() {
		String account = RandomValue.randomStringWithFixedLength(20);
		String secret = server.createSecretFor(account);
		BusinessApp app2 = new BusinessApp(account, secret);
		app2.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2},
			null);
		app2.configSpeedTestSever(speedTestServer);
		app2.start(true);
		app2.setTime(System.currentTimeMillis() / 1000 - 11 * 60);

		String host = RandomValue.randomHost();
		app2.requestResolveHost(host);
		app2.waitForAppThread();

		ArrayList<String> params = new ArrayList<>();
		params.add("s");
		params.add("t");
		params.add("host");
		MatcherAssert.assertThat("超过有效期，服务不处理", !server.getResolveHostServer()
			.hasRequestForArgWithParams(
				ResolveHostServer.ResolveHostArg.create(host, RequestIpType.v4), params, 1,
				false));
	}

	/**
	 * 测试 网络变化后的预解析能力
	 */
	@Test
	@Config(shadows = {ShadowNetworkInfo.class})
	public void testResolveAfterNetworkChanged() {
		app.changeToNetwork(ConnectivityManager.TYPE_WIFI);
		// 先解析域名，使缓存有数据
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		String host4 = RandomValue.randomHost();
		String host5 = RandomValue.randomHost();
		String host6 = RandomValue.randomHost();

		app.requestResolveHost(host1);
		app.requestResolveHost(host2);
		app.requestResolveHost(host3);
		app.requestResolveHost(host4);
		app.requestResolveHost(host5);
		app.requestResolveHost(host6);
		app.waitForAppThread();

		app.enableResolveAfterNetworkChange(true);

		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);
		app.waitForAppThread();
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host1),
			server.getBatchResolveHostServer().getResponseForHost(host1,
				RequestIpType.v4).getHostItem(host1, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host2),
			server.getBatchResolveHostServer().getResponseForHost(host2,
				RequestIpType.v4).getHostItem(host2, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host3),
			server.getBatchResolveHostServer().getResponseForHost(host3,
				RequestIpType.v4).getHostItem(host3, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host4),
			server.getBatchResolveHostServer().getResponseForHost(host4,
				RequestIpType.v4).getHostItem(host4, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host5),
			server.getBatchResolveHostServer().getResponseForHost(host5,
				RequestIpType.v4).getHostItem(host5, RequestIpType.v4).getIps());
		UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名",
			app.requestResolveHost(host6),
			server.getBatchResolveHostServer().getResponseForHost(host6,
				RequestIpType.v4).getHostItem(host6, RequestIpType.v4).getIps());

		app.enableResolveAfterNetworkChange(false);
		app.changeToNetwork(ConnectivityManager.TYPE_WIFI);
		app.waitForAppThread();

		String[] ips = app.requestResolveHost(host3);
		UnitTestUtil.assertIpsEmpty("没有开启网络变化预解析时，网络变换只会清除现有缓存", ips);
	}

	/**
	 * 断网时，不会触发预解析
	 */
	@Test
	@Config(shadows = {ShadowNetworkInfo.class})
	public void testNotResolveWhenNetworkDisconnect() {

		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

		// 先解析域名，使缓存有数据
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		String host4 = RandomValue.randomHost();
		String host5 = RandomValue.randomHost();
		String host6 = RandomValue.randomHost();

		app.requestResolveHost(host1);
		app.requestResolveHost(host2);
		app.requestResolveHost(host3);
		app.requestResolveHost(host4);
		app.requestResolveHost(host5);
		app.requestResolveHost(host6);
		app.waitForAppThread();

		app.enableResolveAfterNetworkChange(true);

		app.changeToNetwork(-1);
		app.waitForAppThread();
		app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);
		app.waitForAppThread();

		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host1), server.getResolveHostServer().getResponse(host1, 1,
				false).get(0).getIps());
		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host2), server.getResolveHostServer().getResponse(host2, 1,
				false).get(0).getIps());
		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host3), server.getResolveHostServer().getResponse(host3, 1,
				false).get(0).getIps());
		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host4), server.getResolveHostServer().getResponse(host4, 1,
				false).get(0).getIps());
		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host5), server.getResolveHostServer().getResponse(host5, 1,
				false).get(0).getIps());
		UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变",
			app.requestResolveHost(host6), server.getResolveHostServer().getResponse(host6, 1,
				false).get(0).getIps());
	}

	/**
	 * 当前服务节点，每天更新一次
	 */
	@Test
	public void serverIpWillUpdateEveryday() {
		String region = REGION_DEFAULT;
		String updateServerResponseFor345 = ServerIpsServer.createUpdateServerResponse(
			new String[] {server.getServerIp(), server1.getServerIp(), server2.getServerIp()},
			RandomValue.randomIpv6s(),
			new int[] {server.getPort(), server1.getPort(), server2.getPort()},
			RandomValue.randomPorts());
		server3.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345,
			-1);
		server4.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345,
			-1);
		server5.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345,
			-1);

		String updateServerResponseFor012 = ServerIpsServer.createUpdateServerResponse(
			new String[] {server3.getServerIp(), server4.getServerIp(), server5.getServerIp()},
			RandomValue.randomIpv6s(),
			new int[] {server3.getPort(), server4.getPort(), server5.getPort()},
			RandomValue.randomPorts());
		server.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012,
			-1);
		server1.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012,
			-1);
		server2.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012,
			-1);

		// 修改region，更新服务IP 到 server 3 4 5
		app.changeRegionTo(region);
		app.waitForAppThread();

		// 因为我们没法模拟时间经过1天，所以直接修改存储的时间到一天前
		app.changeServerIpUpdateTimeTo(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

		// 重置实例
		HttpDns.resetInstance();

		// 重新初始化应用，自动更新服务IP 到 1 2 3
		app.start(false);
		app.waitForAppThread();

		ServerStatusHelper.hasReceiveRegionChange("服务IP超过一天自动更新", app, server3, region,
			true);

		ServerStatusHelper.requestResolveAnotherHost("服务IP更新后使用新的服务解析域名", app,
			server);
	}

	/**
	 * 测试远程降级能力
	 */
	@Test
	public void testDisableService() {
		String region = Constants.REGION_HK;
		String disableResponse = ServerIpsServer.createUpdateServerDisableResponse();
		server.getServerIpsServer().preSetRequestResponse(region, 200, disableResponse, -1);

		// 修改region，触发禁止服务
		app.changeRegionTo(region);
		app.waitForAppThread();

		String host = RandomValue.randomHost();
		String[] ips;
		ips = app.requestResolveHost(host);
		app.waitForAppThread();
		ips = app.requestResolveHost(host);
		app.waitForAppThread();
		UnitTestUtil.assertIpsEmpty("服务禁用之后，不会再解析IP", ips);

		app.requestResolveHostForIpv6(host);
		app.waitForAppThread();
		ips = app.requestResolveHostForIpv6(host);
		UnitTestUtil.assertIpsEmpty("服务禁用之后，不会再解析IP", ips);
	}

	/**
	 * 测试 连续调用时 实际网络请求只会发一次
	 */
	@Test
	public void testMultiThreadForSameHost() {
		int count = 100;
		while (count > 0) {
			count--;
			app.requestResolveHost();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
		ServerStatusHelper.hasReceiveAppResolveHostRequest("同一个域名同时只会请求一次", app,
			server, 1);

		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		String host3 = RandomValue.randomHost();
		ArrayList<String> hostList = new ArrayList<>();
		hostList.add(host1);
		hostList.add(host2);
		hostList.add(host3);
		BatchResolveHostResponse response = BatchResolveHostServer.randomResolveHostResponse(hostList,
			RequestIpType.both);
		server.getBatchResolveHostServer().preSetRequestResponse(
			BatchResolveHostServer.BatchResolveRequestArg.create(hostList, RequestIpType.both), response, 1);
		count = 100;
		while (count > 0) {
			count--;
			app.preResolveHost(hostList, RequestIpType.both);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
		app.waitForAppThread();
	}

	/**
	 * 测试同步接口
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testSyncRequest() throws Throwable {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final Throwable[] exceptions = new Throwable[1];
		new Thread(new Runnable() {
			@Override
			public void run() {
				String host = RandomValue.randomHost();
				ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
					host);
				server.getResolveHostServer().preSetRequestResponse(host, response, 1);
				String[] ips = app.requestResolveHostSync(host);
				try {
					UnitTestUtil.assertIpsEqual("解析结果和预期相同", ips, response.getIps());
				} catch (Throwable e) {
					exceptions[0] = e;
					e.printStackTrace();
				}
				countDownLatch.countDown();
			}
		}).start();
		countDownLatch.await();
		if (exceptions[0] != null) {
			throw exceptions[0];
		}
	}

	/**
	 * 测试频繁调用的情况下，会不会崩溃
	 */
	@Test
	public void testNotCrashWhenCallTwoManyTime() {
		app.checkThreadCount(false);
		String otherRegion = Constants.REGION_HK == REGION_DEFAULT ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		prepareUpdateServerResponse(REGION_DEFAULT, otherRegion);
		try {
			for (int i = 0; i < 1000; i++) {
				String host = RandomValue.randomHost();
				switch (i % 7) {
					case 0:
						app.requestResolveHost(host);
						break;
					case 1:
						app.requestResolveHost();
						break;
					case 2:
						app.requestResolveHostForIpv6(host);
						break;
					case 3:
						app.requestResolveHostForIpv6();
						break;
					case 4:
						app.changeRegionTo(
							RandomValue.randomInt(2) == 0 ? REGION_DEFAULT : otherRegion);
						break;
					case 5:
						ArrayList<String> hostList = new ArrayList<>();
						hostList.add(host);
						hostList.add(RandomValue.randomHost());
						hostList.add(RandomValue.randomHost());
						hostList.add(RandomValue.randomHost());
						hostList.add(RandomValue.randomHost());
						hostList.add(RandomValue.randomHost());
						hostList.add(RandomValue.randomHost());
						app.preResolveHost(hostList,
							RequestIpType.values()[RandomValue.randomInt(3)]);
						break;
					case 6:
						HashMap<String, String> extras = new HashMap<>();
						extras.put("key", "value");
						extras.put("key1", "value1");
						extras.put("key2", "value2");
						app.requestSDNSResolveHost(extras,
							RandomValue.randomStringWithMaxLength(10));
						break;
				}
			}

			for (int i = 0; i < 3; i++) {
				app.waitForAppThread();
				Thread.sleep(1);
			}
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			MatcherAssert.assertThat("调用次数多，不应该崩溃", false);
		}
	}

	/**
	 * 测试 清除指定域名缓存功能
	 */
	@Test
	public void cleanHostCacheWillRemoveLocalCache() {

		app.requestResolveHost();
		app.waitForAppThread();
		// 再次请求，获取服务器返回的结果
		String[] ips = app.requestResolveHost();
		MatcherAssert.assertThat("先解析域名确保有缓存", ips.length > 0 && !ips[0].isEmpty());

		ArrayList<String> hosts = new ArrayList<>();
		hosts.add(app.getRequestHost());
		app.cleanHostCache(hosts);

		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips);
	}

	/**
	 * 测试 清除全部域名缓存功能
	 */
	@Test
	public void cleanHostCacheWithoutHostWillRemoveAllHostCache() {
		String host1 = RandomValue.randomHost();
		String host2 = RandomValue.randomHost();
		app.requestResolveHost(host1);
		app.requestResolveHost(host2);
		app.waitForAppThread();

		String[] ips1 = app.requestResolveHost(host1);
		String[] ips2 = app.requestResolveHost(host2);
		MatcherAssert.assertThat("当前 host1 host2 都有缓存",
			ips1.length > 0 && !ips1[0].isEmpty() && ips2.length > 0 && !ips2[0].isEmpty());

		app.cleanHostCache(null);

		ips1 = app.requestResolveHost(host1);
		ips2 = app.requestResolveHost(host2);
		UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips1);
		UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips2);
	}

	/**
	 * 测试对本地缓存的清理
	 */
	@Test
	public void cleanHostCacheWillCleanCacheInLocalDB() {
		// 启动缓存
		app.enableCache(false);
		// 先发起一些请求，缓存一些Ip结果
		app.requestResolveHost();
		app.waitForAppThread();
		String[] ips1 = app.requestResolveHost();

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		// 读取缓存
		String[] ips2 = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("确认缓存生效", ips1, ips2);
		app.waitForAppThread();
		// 获取新的缓存值
		ips2 = app.requestResolveHost();

		// 重置实例，
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		String[] ips3 = app.requestResolveHost();
		UnitTestUtil.assertIpsEqual("确认缓存没有被清除，一直存在", ips3, ips2);
		app.waitForAppThread();

		ArrayList<String> hosts = new ArrayList<>();
		hosts.add(app.getRequestHost());
		app.cleanHostCache(hosts);

		// 重置实例，
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);
		app.enableCache(false);

		String[] ips4 = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("清除缓存会把数据库缓存也清除", ips4);
	}

	/**
	 * 这个应该手动执行，耗时太长
	 * 测试 多线程并发请求的情况下，接口的耗时情况
	 */
	@Ignore("耗时太长，需要手动执行")
	@Test
	public void multiThreadTest() {
		HttpDnsLog.removeLogger(logger);
		app.setTimeout(10 * 1000);
		HttpDnsLog.enable(false);

		// 测试时总的域名数
		final int hostCount = 10;
		// 会超时的域名数
		final int timeoutCount = 3;
		final String timeoutPrefix = "TIMEOUT";
		final ArrayList<String> hosts = new ArrayList<>(hostCount);
		for (int i = 0; i < hostCount - timeoutCount; i++) {
			hosts.add(RandomValue.randomHost());
		}
		for (int i = 0; i < timeoutCount; i++) {
			hosts.add(timeoutPrefix + RandomValue.randomHost());
		}

		// 预置超时响应
		for (int i = 0; i < hostCount; i++) {
			if (hosts.get(i).startsWith(timeoutPrefix)) {
				server.getResolveHostServer().preSetRequestTimeout(hosts.get(i), -1);
			} else {
				// random response
				server.getResolveHostServer().preSetRequestResponse(hosts.get(i),
					ResolveHostServer.randomResolveHostResponse(hosts.get(i), 5), -1);
			}
		}

		// 并发线程数
		final int threadCount = 10;
		// 测试时长 ms
		final int time = 1 * 60 * 1000;
		// 测试结束锁
		final CountDownLatch testLatch = new CountDownLatch(threadCount);
		final AtomicInteger slowCount = new AtomicInteger(0);
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		// 并发启动锁
		final CountDownLatch startLatch = new CountDownLatch(threadCount);
		for (int i = 0; i < threadCount; i++) {
			service.execute(new Runnable() {
				@Override
				public void run() {
					startLatch.countDown();
					try {
						startLatch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(Thread.currentThread().getId() + " begin");
					int allRequestCount = 0;
					int slowRequestCount = 0;
					int nullResponseCount = 0;
					int emptyResponseCount = 0;
					long longestCostTime = 0;
					long begin = System.currentTimeMillis();
					while (System.currentTimeMillis() - begin < time) {
						String host = hosts.get(RandomValue.randomInt(hostCount));
						long start = System.currentTimeMillis();
						String[] ips = app.requestResolveHost(host);
						long end = System.currentTimeMillis();
						if (end - start > 100) {
							slowRequestCount++;
							if (longestCostTime < end - start) {
								longestCostTime = end - start;
							}
						}
						if (ips == null) {
							nullResponseCount++;
						} else if (ips.length == 0) {
							emptyResponseCount++;
						}
						allRequestCount++;
					}
					System.out.println(
						Thread.currentThread().getId() + " all: " + allRequestCount + ", slow: "
							+ slowRequestCount + ", null: " + nullResponseCount + ", empty: "
							+ emptyResponseCount + ", max : " + longestCostTime);
					slowCount.addAndGet(slowRequestCount);
					testLatch.countDown();
				}
			});
		}
		try {
			testLatch.await();
		} catch (InterruptedException e) {
		}
		MatcherAssert.assertThat("返回慢的调用应该为0", slowCount.get(),
			Matchers.is(Matchers.equalTo(0)));
	}

	/**
	 * https://aone.alibaba-inc.com/req/38989131
	 * <p>
	 * 服务IP不是当前region的服务IP时，不能用于域名解析
	 */
	@Test
	public void stopResolveHostWhenServerIpDoNotBelongCurrentRegion() {
		final String defaultRegion = REGION_DEFAULT;
		final String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;

		// 设置不同region对应的服务信息
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		// 修改region
		app.changeRegionTo(otherRegion);

		// 修改region之后马上请求解析域名
		app.requestResolveHost();

		// 此时 region应该还没有切换完成，域名解析时应该会发现当前服务IP不属于我们设置的region
		// 所以应该 当前服务IP没有接收到解析请求，region更新之后的服务IP也没有接收到解析请求
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest(
			"服务IP和region不匹配, 应该停止解析，直接返回，不应该请求当前服务IP进行解析", app,
			server);
		ServerStatusHelper.hasNotReceiveAppResolveHostRequest(
			"region更新还未完成，应该不会请求到新的服务IP", app, server3);
	}

	/**
	 * 修改region后，马上清除缓存，避免获取错误的IP
	 */
	@Test
	public void changeRegionWillCleanCachePreventGetWrongIp() {
		final String defaultRegion = REGION_DEFAULT;
		final String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		// 设置不同region对应的服务信息
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		app.requestResolveHost();
		app.waitForAppThread();
		String[] ips = app.requestResolveHost();
		// 确定已经有缓存
		MatcherAssert.assertThat("已经有缓存", ips != null && ips.length > 0);

		// 修改region
		app.changeRegionTo(otherRegion);
		// 修改region之后马上请求解析域名
		String[] ipsAfterChagneRegion = app.requestResolveHost();

		MatcherAssert.assertThat("region切换，清除了缓存",
			ipsAfterChagneRegion == null || ipsAfterChagneRegion.length == 0);
	}

	/**
	 * IP缓存仅读取当前region的缓存
	 */
	@Test
	public void cacheWillLoadCurrentRegion() {
		final String defaultRegion = REGION_DEFAULT;
		final String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		// 设置不同region对应的服务信息
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		app.enableCache(false);
		// 先发起一些请求，缓存一些Ip结果
		app.requestResolveHost();
		app.waitForAppThread();
		String[] cachedIps = app.requestResolveHost();
		MatcherAssert.assertThat("确认缓存存在", cachedIps.length > 0);

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();
		app.waitForAppThread();

		// 重启应用，获取新的实例
		app.start(true);
		// 切换region
		app.changeRegionTo(otherRegion);
		// 再加载缓存
		app.enableCache(false);

		String[] ips = app.requestResolveHost();
		MatcherAssert.assertThat("缓存被清除了，此时返回是空", ips == null || ips.length == 0);
	}

	/**
	 * 通过初始化 开启IP缓存
	 */
	@Test
	public void enableCacheWhenInit() {
		String accountId = RandomValue.randomStringWithFixedLength(10);

		new InitConfig.Builder()
			.setEnableCacheIp(true)
			.buildFor(accountId);

		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app.start(true);

		// 先发起一些请求，缓存一些Ip结果
		app.requestResolveHost();
		app.waitForAppThread();
		String[] ips = app.requestResolveHost();
		MatcherAssert.assertThat("确定有缓存了", ips.length > 0);
		ServerStatusHelper.hasReceiveAppResolveHostRequest("读取缓存之前请求了一次服务器", app,
			server, 1);

		// 重置实例，确保下次读取的信息是从本地缓存来的
		HttpDns.resetInstance();

		// 重启应用，获取新的实例
		app.start(true);

		String[] ips2 = app.requestResolveHost();
		ServerStatusHelper.hasReceiveAppResolveHostRequest(
			"有缓存，但是由于无法判断之前和之后的网络是否一致，还是需要再请求一次", app, server, 2);
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips2, ips);
	}

	/**
	 * 通过初始化设置region
	 */
	@Test
	public void setRegionWhenInit() {
		String accountId = RandomValue.randomStringWithFixedLength(10);

		new InitConfig.Builder()
			.setRegion(Constants.REGION_HK)
			.buildFor(accountId);

		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);

		app.start(false);

		ServerStatusHelper.hasReceiveRegionChange("初始化时更新HK节点, 一次", app, server,
			Constants.REGION_HK, 1, true);
	}

	/**
	 * 通过初始化 禁用过期IP
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void disableExpireIpWhenInit() throws InterruptedException {
		String accountId = RandomValue.randomStringWithFixedLength(10);

		new InitConfig.Builder()
			.setEnableExpiredIp(false)
			.buildFor(accountId);

		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app.start(true);

		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost(), 1);
		ResolveHostResponse response1 = ResolveHostServer.randomResolveHostResponse(
			app.getRequestHost());
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
		server.getResolveHostServer().preSetRequestResponse(app.getRequestHost(), response1, -1);
		// 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
		String[] ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
		app.waitForAppThread();
		// 再次请求，获取服务器返回的结果
		app.requestResolveHost();

		Thread.sleep(1000);
		// ttl 过期后请求ip
		ips = app.requestResolveHost();
		UnitTestUtil.assertIpsEmpty("不启用过期IP，返回空", ips);
		ServerStatusHelper.hasReceiveAppResolveHostRequestWithResult(
			"ttl过期后，请求会触发网络请求", app,
			ResolveHostServer.ResolveHostArg.create(app.getRequestHost()), server, response1
			, 1,
			true);
		// 再次请求，获取再次请求服务器返回的结果
		ips = app.requestResolveHost();
		// 结果和服务器返回一致
		UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response1.getIps(), ips);
	}

	/**
	 * 通过初始化设置超时
	 */
	@Test
	public void setTimeoutWhenInit() {
		String accountId = RandomValue.randomStringWithFixedLength(10);
		// 设置超时时间
		int timeout = 1000;
		new InitConfig.Builder()
			.setTimeout(timeout)
			.buildFor(accountId);
		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app.start(true);

		// 预设请求超时
		server.getResolveHostServer().preSetRequestTimeout(app.getRequestHost(), -1);

		// 请求 并计时
		long start = System.currentTimeMillis();
		app.requestResolveHost();
		// 确实是否接受到请求，并超时
		ServerStatusHelper.hasReceiveAppResolveHostRequestButTimeout(app, server);
		long costTime = System.currentTimeMillis() - start;

		// 3.05 是个经验数据，可以考虑调整。影响因素主要有重试次数和线程切换
		assertThat("requst timeout " + costTime, costTime < timeout * 3.05);
	}

	/**
	 * 通过初始化设置测速配置
	 */
	@Test
	public void configProbeWhenInit() {
		speedTestServer.watch(server);
		String accountId = RandomValue.randomStringWithFixedLength(10);
		String host = RandomValue.randomHost();
		new InitConfig.Builder()
			.setIPRankingList(Arrays.asList(new IPRankingBean(host, 6666)))
			.buildFor(accountId);

		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app.start(true);

		// 请求数据触发IP优选
		app.requestResolveHost(host);
		app.waitForAppThread();

		// 判断返回的结果是优选的结果
		String[] ips = app.requestResolveHost(host);
		String[] sortedIps = speedTestServer.getSortedIpsFor(host);

		UnitTestUtil.assertIpsEqual("设置ip优选后，返回的ip是优选之后的结果", ips, sortedIps);
	}

	/**
	 * 加载本地缓存应该在region切换之后，避免加载错误的缓存
	 */
	@Test
	public void loadCacheAfterRegionChangeWhenInit() {
		final String defaultRegion = REGION_DEFAULT;
		final String otherRegion = Constants.REGION_HK == defaultRegion ? Constants.REGION_MAINLAND
			: Constants.REGION_HK;
		// 设置不同region对应的服务信息
		prepareUpdateServerResponse(defaultRegion, otherRegion);

		String accountId = RandomValue.randomStringWithFixedLength(10);
		new InitConfig.Builder()
			.setEnableCacheIp(true)
			.buildFor(accountId);
		BusinessApp app = new BusinessApp(accountId);
		app.configInitServer(REGION_DEFAULT, new HttpDnsServer[] {server, server1, server2}, null);
		app.configSpeedTestSever(speedTestServer);
		app.start(true);

		app.requestResolveHost();
		app.waitForAppThread();
		String[] ips = app.requestResolveHost();
		// 确定已经有缓存
		MatcherAssert.assertThat("已经有缓存", ips != null && ips.length > 0);

		HttpDns.resetInstance();
		app.waitForAppThread();

		new InitConfig.Builder()
			.setEnableCacheIp(true)
			.setRegion(otherRegion)
			.buildFor(accountId);
		// 这里上个实例刚更新过服务节点，所以本地启动不会更新服务节点
		app.start(false);

		app.waitForAppThread();

		String[] ipsAfterChangeRegion = app.requestResolveHost();
		MatcherAssert.assertThat("region切换，无法读取到原region的缓存",
			ipsAfterChangeRegion == null || ipsAfterChangeRegion.length == 0);

		HttpDns.resetInstance();
		app.waitForAppThread();

		new InitConfig.Builder()
			.setEnableCacheIp(true)
			.setRegion(otherRegion)
			.buildFor(accountId);
		// 这里上个实例刚更新过服务节点，所以本地启动不会更新服务节点
		app.start(false);

		app.waitForAppThread();
		String[] ipsWhenRegionNotChange = app.requestResolveHost();
		MatcherAssert.assertThat("region保持一致，读取到原region的缓存",
			ipsWhenRegionNotChange != null && ipsWhenRegionNotChange.length > 0);
	}
}
