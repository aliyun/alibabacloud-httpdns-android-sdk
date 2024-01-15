package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;

/**
 * 请求失败时，切换当前Region的服务IP，服务IP都切换过，更新服务IP
 */
public class ShiftServerWatcher implements HttpRequestWatcher.Watcher {

	private final HttpDnsConfig mHttpDnsConfig;
	private final RegionServerScheduleService mScheduleService;
	private final StatusControl mStatusControl;
	private long mBeginRequestTime;

	public ShiftServerWatcher(HttpDnsConfig config, RegionServerScheduleService scheduleService,
							  StatusControl statusControl) {
		this.mHttpDnsConfig = config;
		this.mScheduleService = scheduleService;
		this.mStatusControl = statusControl;
	}

	@Override
	public void onStart(HttpRequestConfig config) {
		mBeginRequestTime = System.currentTimeMillis();
	}

	@Override
	public void onSuccess(HttpRequestConfig requestConfig, Object data) {
		if (requestConfig.getIpType() == RequestIpType.v6) {
			if (this.mHttpDnsConfig.getCurrentServer().markOkServerV6(requestConfig.getIp(),
				requestConfig.getPort())) {
				if (mStatusControl != null) {
					mStatusControl.turnUp();
				}
			}
		} else {
			if (this.mHttpDnsConfig.getCurrentServer().markOkServer(requestConfig.getIp(),
				requestConfig.getPort())) {
				if (mStatusControl != null) {
					mStatusControl.turnUp();
				}
			}
		}

	}

	@Override
	public void onFail(HttpRequestConfig requestConfig, Throwable throwable) {
		long cost = System.currentTimeMillis() - mBeginRequestTime;
		// 是否切换服务IP, 超过超时时间，我们也切换ip，花费时间太长，说明这个ip可能也有问题
		if (shouldShiftServer(throwable) || cost > requestConfig.getTimeout()) {
			// 切换和更新请求的服务IP
			boolean isBackToFirstServer;
			if (requestConfig.getIpType() == RequestIpType.v6) {
				isBackToFirstServer = this.mHttpDnsConfig.getCurrentServer().shiftServerV6(
					requestConfig.getIp(), requestConfig.getPort());
				requestConfig.setIp(this.mHttpDnsConfig.getCurrentServer().getServerIpForV6());
				requestConfig.setPort(this.mHttpDnsConfig.getCurrentServer().getPortForV6());
			} else {
				isBackToFirstServer = this.mHttpDnsConfig.getCurrentServer().shiftServer(
					requestConfig.getIp(), requestConfig.getPort());
				requestConfig.setIp(this.mHttpDnsConfig.getCurrentServer().getServerIp());
				requestConfig.setPort(this.mHttpDnsConfig.getCurrentServer().getPort());
			}

			// 所有服务IP都尝试过了，通知上层进一步处理
			if (isBackToFirstServer && mScheduleService != null) {
				mScheduleService.updateRegionServerIps();
			}
			if (mStatusControl != null) {
				mStatusControl.turnDown();
			}
		}
	}

	private boolean shouldShiftServer(Throwable throwable) {
		if (throwable instanceof HttpException) {
			return ((HttpException)throwable).shouldShiftServer();
		}
		// 除了特定的一些错误（sdk问题或者客户配置问题，不是服务网络问题），都切换服务IP，这边避免个别服务节点真的访问不上
		// 一方面尽可能提高可用性，另一方面当客户发生异常时，也方便根据服务IP来判断是否是真的无网络，因为多个服务IP都访问不上的可能性较低
		// 还有一方面是 sniff模式是在切换服务IP的前提下触发的，这样也提高的sniff模式触发几率，在真的网络异常时，降低网络请求频次
		return true;
	}
}
