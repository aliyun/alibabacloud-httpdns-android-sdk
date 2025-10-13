package com.alibaba.sdk.android.httpdns.exception;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

public class HttpDnsUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	// 处理所有未捕获异常
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			HttpDnsLog.e("Catch an uncaught exception, " + thread.getName() + ", error message: "
				+ ex.getMessage(), ex);
			reportUncaughtError(ex);
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void reportUncaughtError(Throwable ex) {

	}
}
