package com.alibaba.sdk.android.httpdns.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.impl.SignService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.serverip.RegionServerScheduleService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 发起域名解析请求
 */
public class ResolveHostRequestHandler {

	private final HttpDnsConfig mHttpDnsConfig;
	private final RegionServerScheduleService mScheduleService;
	private final CategoryController mCategoryController;
	private final HashMap<String, String> mGlobalParams;
	private final SignService mSignService;

	public ResolveHostRequestHandler(HttpDnsConfig config, RegionServerScheduleService scheduleService,
									 SignService signService) {
		this.mHttpDnsConfig = config;
		this.mScheduleService = scheduleService;
		this.mCategoryController = new CategoryController(scheduleService);
		this.mGlobalParams = new HashMap<>();
		this.mSignService = signService;
	}

	public void requestResolveHost(final String host, final RequestIpType type,
								   Map<String, String> extras, final String cacheKey,
								   RequestCallback<ResolveHostResponse> callback) {
		HttpRequestConfig requestConfig = ResolveHostHelper.getConfig(mHttpDnsConfig, host, type,
			extras, cacheKey,
			mGlobalParams, mSignService);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("start resolve ip request for " + host + " " + type);
		}
		mCategoryController.getCategory().resolve(mHttpDnsConfig, requestConfig, callback);
	}

	public void requestResolveHost(final ArrayList<String> hostList, final RequestIpType type,
								   RequestCallback<BatchResolveHostResponse> callback) {
		HttpRequestConfig requestConfig = ResolveHostHelper.getConfig(mHttpDnsConfig, hostList,
			type,
			mSignService);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("start resolve hosts async for " + hostList.toString() + " " + type);
		}

		HttpRequest<BatchResolveHostResponse> request = new HttpRequest<>(
			requestConfig, new BatchResolveHostResponseParser());
		request = new HttpRequestWatcher<>(request,
			new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(
				mHttpDnsConfig.getAccountId())));
		// 切换服务IP，更新服务IP
		request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(mHttpDnsConfig,
			mScheduleService, mCategoryController));
		// 重试一次
		request = new RetryHttpRequest<>(request, 1);
		try {
			mHttpDnsConfig.getResolveWorker().execute(
				new HttpRequestTask<>(request, callback));
		} catch (Throwable e) {
			callback.onFail(e);
		}
	}

	/**
	 * 重置状态
	 */
	public void resetStatus() {
		mCategoryController.reset();
	}

	/**
	 * 设置嗅探模式的请求时间间隔
	 */
	public void setSniffTimeInterval(int timeInterval) {
		mCategoryController.setSniffTimeInterval(timeInterval);
	}

	/**
	 * 设置sdns的全局参数
	 */
	public void setSdnsGlobalParams(Map<String, String> params) {
		this.mGlobalParams.clear();
		if (params != null) {
			this.mGlobalParams.putAll(params);
		}
	}

	/**
	 * 清除sdns的全局参数
	 */
	public void clearSdnsGlobalParams() {
		this.mGlobalParams.clear();
	}

}
