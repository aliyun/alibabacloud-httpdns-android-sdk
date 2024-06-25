package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 嗅探模式
 */
public class SniffResolveCategory implements ResolveHostCategory {

	private final RegionServerScheduleService mScheduleService;
	private final StatusControl mStatusControl;
	private int mTimeInterval = 30 * 1000;
	private long mLastRequestTime = 0L;

	public SniffResolveCategory(RegionServerScheduleService scheduleService, StatusControl statusControl) {
		this.mScheduleService = scheduleService;
		this.mStatusControl = statusControl;
	}

	@Override
	public void resolve(HttpDnsConfig config, HttpRequestConfig requestConfig,
						RequestCallback<ResolveHostResponse> callback) {
		long currentTimeMillis = System.currentTimeMillis();
		// 请求间隔 不小于timeInterval
		if (currentTimeMillis - mLastRequestTime < mTimeInterval) {
			callback.onFail(Constants.sniff_too_often);
			return;
		}
		mLastRequestTime = currentTimeMillis;

		HttpRequest<ResolveHostResponse> request = new HttpRequest<>(
            requestConfig, new ResolveInterpretHostResponseParser());
		request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(
			ReportManager.getReportManagerByAccount(config.getAccountId())));
		// 切换服务IP，更新服务IP
		request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config,
            mScheduleService,
			mStatusControl));
		try {
			config.getResolveWorker().execute(new HttpRequestTask<>(request, callback));
		} catch (Throwable tr) {
			callback.onFail(tr);
		}
	}

	public void setInterval(int timeMs) {
		this.mTimeInterval = timeMs;
	}

	public void reset() {
		mLastRequestTime = 0L;
	}
}
