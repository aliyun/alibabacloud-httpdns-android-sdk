package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;

/**
 * 域名解析的一般策略
 */
public class NormalResolveCategory implements ResolveHostCategory {

	private final StatusControl mStatusControl;
	private final RegionServerScheduleService mScheduleService;

	public NormalResolveCategory(RegionServerScheduleService scheduleService, StatusControl statusControl) {
		this.mScheduleService = scheduleService;
		this.mStatusControl = statusControl;
	}

	@Override
	public void resolve(HttpDnsConfig config, HttpRequestConfig requestConfig,
						RequestCallback<ResolveHostResponse> callback) {
		HttpRequest<ResolveHostResponse> request = new HttpRequest<>(requestConfig,
			new ResolveInterpretHostResponseParser());
		request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(
			ReportManager.getReportManagerByAccount(config.getAccountId())));
		// 切换服务IP，更新服务IP
		request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config,
            mScheduleService,
			mStatusControl));
		// 重试一次
		request = new RetryHttpRequest<>(request, 1);
		try {
			config.getWorker().execute(new HttpRequestTask<>(request, callback));
		} catch (Throwable e) {
			callback.onFail(e);
		}
	}

}
