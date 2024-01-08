package com.alibaba.sdk.android.httpdns.test.helper;

import com.alibaba.sdk.android.httpdns.resolve.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.ResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 服务状态判读辅助类
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class ServerStatusHelper {

	/**
	 * 判断服务是否接收到域名解析请求
	 *
	 * @param app
	 */

	public static void hasReceiveAppResolveHostRequest(String reason, BusinessApp app,
													   HttpDnsServer server, int count) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		assertThat(reason + "， count: " + count,
			server.getResolveHostServer().hasRequestForArg(requestHost, count, false));
	}

	public static void hasReceiveAppResolveHostRequestWithDegrade(BusinessApp app,
																  HttpDnsServer server) {
		hasReceiveAppResolveHostRequestWithResult(app, server, HttpException.ERROR_CODE_403,
			HttpException.ERROR_MSG_SERVICE_LEVEL_DENY, -1, false);
	}

	public static void hasReceiveAppResolveHostRequestWithResult(BusinessApp app,
																 HttpDnsServer server,
																 int httpcode,
																 String body) {
		hasReceiveAppResolveHostRequestWithResult(app, server, httpcode, body, -1, false);
	}

	public static void hasReceiveAppResolveHostRequestWithResult(BusinessApp app,
																 HttpDnsServer server,
																 int httpcode,
																 String body, int count,
																 boolean remove) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		assertThat("server should got request with result, code " + httpcode + " body " + body,
			server.getResolveHostServer()
				.hasRequestForArgWithResult(requestHost, httpcode, body, count, remove));
	}

	public static void hasReceiveAppResolveHostRequestWithResult(String reason, BusinessApp app,
																 HttpDnsServer server,
																 ResolveHostResponse response) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		assertThat(reason, server.getResolveHostServer()
			.hasRequestForArgWithResult(requestHost, response, -1, false));
	}

	public static void hasReceiveAppResolveHostRequestWithResult(String reason, BusinessApp app,
																 ResolveHostServer.ResolveHostArg arg,
																 HttpDnsServer server,
																 ResolveHostResponse response,
																 int count, boolean removeRecord) {
		app.waitForAppThread();
		assertThat(reason, server.getResolveHostServer()
			.hasRequestForArgWithResult(arg, response, count, removeRecord));
	}

	/**
	 * 判断服务器 没有收到解析请求
	 *
	 * @param reason
	 * @param app
	 * @param server
	 */
	public static void hasNotReceiveAppResolveHostRequest(String reason, BusinessApp app,
														  HttpDnsServer server) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		assertThat(reason, !server.getResolveHostServer().hasRequestForArg(requestHost, -1,
			false));
	}

	/**
	 * 判断服务器 没有收到解析请求
	 *
	 * @param reason
	 * @param app
	 * @param arg
	 * @param server
	 */
	public static void hasNotReceiveAppResolveHostRequest(String reason, BusinessApp app,
														  ResolveHostServer.ResolveHostArg arg,
														  HttpDnsServer server) {
		app.waitForAppThread();
		assertThat(reason, !server.getResolveHostServer().hasRequestForArg(arg, -1, false));
	}

	/**
	 * 获取服务器返回的数据
	 *
	 * @param app
	 * @param server
	 * @return
	 */
	public static String[] getServerResponseIps(BusinessApp app, HttpDnsServer server) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		List<ResolveHostResponse> responses =
			server.getResolveHostServer().getResponse(requestHost,
			1, true);
		assertThat("server do not get request", responses.size() == 1);
		return responses.get(0).getIps();
	}

	public static void hasReceiveAppResolveHostRequestButTimeout(BusinessApp app,
																 HttpDnsServer server) {
		app.waitForAppThread();
		String requestHost = app.getRequestHost();
		assertThat("server should got request",
			server.getResolveHostServer().hasRequestForArgTimeout(requestHost, -1, false));
	}

	/**
	 * 判断服务是否接收到服务IP更新请求
	 *
	 * @param app
	 * @param region
	 */
	public static void hasReceiveRegionChange(String reason, BusinessApp app, HttpDnsServer server,
											  String region) {
		app.waitForAppThread();
		assertThat(reason + ", region " + region,
			server.getServerIpsServer().hasRequestForArg(region, 1, false));
	}

	public static void hasReceiveRegionChange(String reason, BusinessApp app, HttpDnsServer server,
											  String region, boolean remove) {
		app.waitForAppThread();
		assertThat(reason + ", region " + region,
			server.getServerIpsServer().hasRequestForArg(region, -1, remove));
	}

	public static void hasReceiveRegionChange(String reason, BusinessApp app, HttpDnsServer server,
											  String region, int count, boolean remove) {
		app.waitForAppThread();
		assertThat(reason + ", region " + region,
			server.getServerIpsServer().hasRequestForArg(region, count, remove));
	}

	public static void hasNotReceiveRegionChange(String reason, BusinessApp app,
												 HttpDnsServer server, String region) {
		app.waitForAppThread();
		assertThat(reason + ", region " + region,
			!server.getServerIpsServer().hasRequestForArg(region, -1, false));
	}

	/**
	 * 请求解析域名，判断服务是否收到
	 *
	 * @param app
	 * @param server
	 */
	public static void requestResolveAnotherHost(String reason, BusinessApp app,
												 HttpDnsServer server) {
		String host = RandomValue.randomHost();
		ResolveHostResponse response = ResolveHostServer.randomResolveHostResponse(host);
		server.getResolveHostServer().preSetRequestResponse(host, response, 1);
		app.requestResolveHost(host);
		app.waitForAppThread();
		assertThat(reason,
			server.getResolveHostServer().hasRequestForArgWithResult(host, response, 1, false));
	}

	/**
	 * 对于 {@link HttpException#ERROR_MSG_SERVICE_LEVEL_DENY} 这个错误之前理解有误
	 * 之前以为含义是服务节点下线了，不可用，才会返回此错误码，所以此方法命名为降级服务。
	 * 真实含义是当前用户不能使用此服务节点。
	 * 但是从效果上来说是一样的，都是返回了一个错误，此错误应该触发的逻辑是 切换服务节点。 所以还是保留此命名
	 *
	 * @param server
	 * @param requestHost
	 * @param count
	 */
	public static void degradeServer(HttpDnsServer server, String requestHost, int count) {
		server.getResolveHostServer().preSetRequestResponse(requestHost,
			HttpException.ERROR_CODE_403, HttpException.ERROR_MSG_SERVICE_LEVEL_DENY, count);
	}

	public static void setError(HttpDnsServer server, String requestHost, int httpCode,
								String httpMsg, int count) {
		server.getResolveHostServer().preSetRequestResponse(requestHost, httpCode, httpMsg, count);
	}
}
