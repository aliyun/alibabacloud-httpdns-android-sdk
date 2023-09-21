package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.report.ReportUtils;

public class HttpRequestFailWatcher implements HttpRequestWatcher.Watcher {

	ReportManager reportManager;

	public HttpRequestFailWatcher(ReportManager reportManager) {
		this.reportManager = reportManager;
	}

	@Override
	public void onStart(HttpRequestConfig config) {

	}

	@Override
	public void onSuccess(HttpRequestConfig config, Object data) {

	}

	@Override
	public void onFail(HttpRequestConfig config, Throwable throwable) {
		if (reportManager != null) {
			if (config.url().contains("/ss")) {
				reportManager.reportErrorSCRequest(config.getIp(),
					ReportUtils.getErrorCode(throwable) + "", ReportUtils.getErrorMsg(throwable),
					0);
			} else {
				reportManager.reportErrorHttpDnsRequest(config.getIp(),
					ReportUtils.getErrorCode(throwable) + "", ReportUtils.getErrorMsg(throwable)
                    , 0,
					0);
			}
		}
	}
}
