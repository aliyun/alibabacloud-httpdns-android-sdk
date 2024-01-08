package com.alibaba.sdk.android.httpdns.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.sdk.android.httpdns.exception.HttpDnsUncaughtExceptionHandler;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

public class ThreadUtil {
	private static int index = 0;

	public static ExecutorService createSingleThreadService(final String tag) {
		final ThreadPoolExecutor httpDnsThread = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(4), new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, tag + index++);
				thread.setPriority(Thread.NORM_PRIORITY - 1);
				thread.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
				return thread;
			}
		}, new ThreadPoolExecutor.AbortPolicy());
		return new ExecutorServiceWrapper(httpDnsThread);
	}

	public static ExecutorService createExecutorService() {
		final ThreadPoolExecutor httpDnsThread = new ThreadPoolExecutor(0, 10, 30,
			TimeUnit.SECONDS,
			new SynchronousQueue<>(), new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "httpdns" + index++);
				thread.setPriority(Thread.NORM_PRIORITY - 1);
				thread.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
				return thread;
			}
		}, new ThreadPoolExecutor.AbortPolicy());
		return new ExecutorServiceWrapper(httpDnsThread);
	}

	public static ExecutorService createDBExecutorService() {
		final ThreadPoolExecutor httpDnsThread = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(4), new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "httpdns_db" + index++);
				thread.setPriority(Thread.NORM_PRIORITY - 1);
				thread.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
				return thread;
			}
		}, new ThreadPoolExecutor.AbortPolicy());
		return new ExecutorServiceWrapper(httpDnsThread);
	}

	private static class ExecutorServiceWrapper implements ExecutorService {
		private final ThreadPoolExecutor mHttpDnsThread;

		public ExecutorServiceWrapper(ThreadPoolExecutor httpdnsThread) {
			this.mHttpDnsThread = httpdnsThread;
		}

		@Override
		public void shutdown() {
			mHttpDnsThread.shutdown();
		}

		@Override
		public List<Runnable> shutdownNow() {
			return mHttpDnsThread.shutdownNow();
		}

		@Override
		public boolean isShutdown() {
			return mHttpDnsThread.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return mHttpDnsThread.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return mHttpDnsThread.awaitTermination(timeout, unit);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			try {
				return mHttpDnsThread.submit(task);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			try {
				return mHttpDnsThread.submit(task, result);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public Future<?> submit(Runnable task) {
			try {
				return mHttpDnsThread.submit(task);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
			try {
				return mHttpDnsThread.invokeAll(tasks);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
											 TimeUnit unit) throws InterruptedException {
			try {
				return mHttpDnsThread.invokeAll(tasks, timeout, unit);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws ExecutionException, InterruptedException {
			try {
				return mHttpDnsThread.invokeAny(tasks);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
							   TimeUnit unit)
			throws ExecutionException, InterruptedException, TimeoutException {
			try {
				return mHttpDnsThread.invokeAny(tasks, timeout, unit);
			} catch (RejectedExecutionException e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}

		@Override
		public void execute(Runnable command) {
			try {
				mHttpDnsThread.execute(command);
			} catch (Exception e) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("too many request ?", e);
				}
				throw e;
			}
		}
	}
}
