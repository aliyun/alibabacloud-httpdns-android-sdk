package com.alibaba.sdk.android.httpdns.report;

import java.net.SocketTimeoutException;

import com.alibaba.sdk.android.httpdns.request.HttpException;

import android.text.TextUtils;

public class ReportUtils {
	public static int getErrorCode(Throwable throwable) {
		if (throwable instanceof HttpException) {
			return ((HttpException)throwable).getCode();
		} else if (throwable instanceof SocketTimeoutException) {
			return ReportConfig.ERROR_CODE_TIMEOUT;
		} else {
			return ReportConfig.ERROR_CODE_DEFAULT;
		}
	}

	public static String getErrorMsg(Throwable throwable) {
		if (throwable != null && !TextUtils.isEmpty(throwable.getMessage())) {
			return throwable.getMessage();
		} else {
			if (throwable instanceof SocketTimeoutException) {
				return ReportConfig.ERROR_MSG_TIMEOUT;
			} else {
				return ReportConfig.ERROR_MSG_DEFAULT;
			}
		}
	}
}
