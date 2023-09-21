package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.ApiForTest;
import com.alibaba.sdk.android.httpdns.BeforeHttpDnsServiceInit;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.InitManager;
import com.alibaba.sdk.android.httpdns.probe.ProbeTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 测试时 使用的httpdns 实例
 * 增加了一些用于测试的api和机制
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class HttpDnsServiceTestImpl extends HttpDnsServiceImpl implements ApiForTest {
	public HttpDnsServiceTestImpl(Context context, String accountId, String secret) {
		super(context, accountId, secret);
	}

	@Override
	protected void beforeInit() {
		super.beforeInit();
		// 通过InitManager 在httpdns初始化之前 进行一些测试需要前置工作
		BeforeHttpDnsServiceInit init = InitManager.getInstance().getAndRemove(
			mHttpDnsConfig.getAccountId());

		if (init != null) {
			init.beforeInit(this);
		}
	}

	@Override
	protected void initCrashDefend(Context context, HttpDnsConfig config) {
		// do nothing for test
		mHttpDnsConfig.crashDefend(false);
	}

	@Override
	public void setInitServer(String region, String[] ips, int[] ports, String[] ipv6s,
							  int[] v6Ports) {
		mHttpDnsConfig.setInitServers(region, ips, ports, ipv6s, v6Ports);
	}

	@Override
	public void setThread(ScheduledExecutorService scheduledExecutorService) {
		mHttpDnsConfig.setWorker(scheduledExecutorService);
	}

	@Override
	public void setSocketFactory(ProbeTask.SpeedTestSocketFactory speedTestSocketFactory) {
		mIpProbeService.setSocketFactory(speedTestSocketFactory);
	}

	@Override
	public void setUpdateServerTimeInterval(int timeInterval) {
		mScheduleService.setTimeInterval(timeInterval);
	}

	@Override
	public void setSniffTimeInterval(int timeInterval) {
		mRequestHandler.setSniffTimeInterval(timeInterval);
	}

	@Override
	public ExecutorService getWorker() {
		return mHttpDnsConfig.mWorker;
	}

	@Override
	public void setDefaultUpdateServer(String[] defaultServerIps, int[] ports) {
		mHttpDnsConfig.setDefaultUpdateServer(defaultServerIps, ports);
	}

	@Override
	public void setDefaultUpdateServerIpv6(String[] defaultServerIps, int[] ports) {
		mHttpDnsConfig.setDefaultUpdateServerIpv6(defaultServerIps, ports);
	}

	@Override
	public void setNetworkDetector(HttpDnsSettings.NetworkDetector networkDetector) {
		mHttpDnsConfig.setNetworkDetector(networkDetector);
	}
}
