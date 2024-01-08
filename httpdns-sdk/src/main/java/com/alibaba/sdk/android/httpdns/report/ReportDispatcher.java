package com.alibaba.sdk.android.httpdns.report;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.httpdns.exception.HttpDnsUncaughtExceptionHandler;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

final class ReportDispatcher {

	private static final String DEMO_NAME = "report_thread";

	private final ThreadFactory mThreadFactory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread result = new Thread(runnable, DEMO_NAME);
			result.setDaemon(false);
			result.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
			return result;
		}
	};

	private ExecutorService mExecutorService;

	synchronized ExecutorService getExecutorService() {
		if (mExecutorService == null) {
			mExecutorService = new ThreadPoolExecutor(0, 10, 1, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), mThreadFactory, new MyDiscardPolicy());
		}
		return mExecutorService;
	}

	private static class MyDiscardPolicy extends ThreadPoolExecutor.DiscardPolicy {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			super.rejectedExecution(r, e);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("too many report? drop it!");
			}
		}
	}

}
