package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.SingleResolveHttpRequestStatusWatcher;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 嗅探模式
 */
public class SniffResolveCategory implements ResolveHostCategory {
	private final HttpDnsConfig mHttpDnsConfig;
	private final RegionServerScheduleService mScheduleService;
	private final StatusControl mStatusControl;
	private int mTimeInterval = 30 * 1000;
	private long mLastRequestTime = 0L;

	public SniffResolveCategory(HttpDnsConfig config, RegionServerScheduleService scheduleService, StatusControl statusControl) {
		this.mScheduleService = scheduleService;
		this.mStatusControl = statusControl;
		mHttpDnsConfig = config;
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
            requestConfig, new ResolveHostResponseParser(requestConfig.getAESEncryptService()));
		request = new HttpRequestWatcher<>(request, new SingleResolveHttpRequestStatusWatcher(
			mHttpDnsConfig.getObservableManager()));
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
