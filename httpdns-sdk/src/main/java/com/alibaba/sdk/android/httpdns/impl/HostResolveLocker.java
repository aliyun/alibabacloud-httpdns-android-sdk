package com.alibaba.sdk.android.httpdns.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.httpdns.RequestIpType;

public class HostResolveLocker {

	private static class Recorder {
		private final HashSet<String> mV4ResolvingHost = new HashSet<>();
		private final HashSet<String> mV6ResolvingHost = new HashSet<>();
		private final HashSet<String> mBothResolvingHost = new HashSet<>();
		private final HashMap<String, CountDownLatch> mV4Latchs = new HashMap<>();
		private final HashMap<String, CountDownLatch> mV6Latchs = new HashMap<>();
		private final HashMap<String, CountDownLatch> mBothLatchs = new HashMap<>();
		private final Object mLock = new Object();

		public boolean beginResolve(String host, RequestIpType type) {

			if (type == RequestIpType.both) {
				if (mBothResolvingHost.contains(host)) {
					// 正在解析
					return false;
				} else {
					synchronized (mLock) {
						if (mBothResolvingHost.contains(host)) {
							// 正在解析
							return false;
						} else {
							mBothResolvingHost.add(host);
							createLatch(host, mBothLatchs);
							return true;
						}
					}
				}
			} else if (type == RequestIpType.v4) {
				if (mV4ResolvingHost.contains(host)) {
					return false;
				} else {
					synchronized (mLock) {
						if (mV4ResolvingHost.contains(host)) {
							return false;
						} else {
							mV4ResolvingHost.add(host);
							createLatch(host, mV4Latchs);
							return true;
						}
					}
				}
			} else if (type == RequestIpType.v6) {
				if (mV6ResolvingHost.contains(host)) {
					return false;
				} else {
					synchronized (mLock) {
						if (mV6ResolvingHost.contains(host)) {
							return false;
						} else {
							mV6ResolvingHost.add(host);
							createLatch(host, mV6Latchs);
							return true;
						}
					}
				}
			}
			return false;
		}

		private void createLatch(String host, HashMap<String, CountDownLatch> latchs) {
			CountDownLatch countDownLatch = new CountDownLatch(1);
			latchs.put(host, countDownLatch);
		}

		private void countDownLatch(String host, HashMap<String, CountDownLatch> latchs) {
			CountDownLatch latch = latchs.get(host);
			if (latch != null) {
				latch.countDown();
			}
		}

		private CountDownLatch getLatch(String host, RequestIpType type) {
			switch (type) {
				case v4:
					return mV4Latchs.get(host);
				case v6:
					return mV6Latchs.get(host);
				case both:
					return mBothLatchs.get(host);
			}
			return null;
		}

		public void endResolve(String host, RequestIpType type) {
			switch (type) {
				case v4:
					mV4ResolvingHost.remove(host);
					countDownLatch(host, mV4Latchs);
					break;
				case v6:
					mV6ResolvingHost.remove(host);
					countDownLatch(host, mV6Latchs);
					break;
				case both:
					mBothResolvingHost.remove(host);
					countDownLatch(host, mBothLatchs);
					break;
			}
		}

		public boolean await(String host, RequestIpType type, long timeout, TimeUnit unit)
			throws InterruptedException {
			CountDownLatch countDownLatch = getLatch(host, type);
			if (countDownLatch != null) {
				return countDownLatch.await(timeout, unit);
			} else {
				return true;
			}
		}
	}

	private final Object mLock = new Object();
	private final Recorder mDefaultRecorder = new Recorder();
	private final HashMap<String, Recorder> mRecorders = new HashMap<>();

	public boolean beginResolve(String host, RequestIpType type, String cacheKey) {
		Recorder recorder = getRecorder(cacheKey);
		return recorder.beginResolve(host, type);
	}

	private Recorder getRecorder(String cacheKey) {
		Recorder recorder;
		if (cacheKey == null || cacheKey.isEmpty()) {
			recorder = mDefaultRecorder;
		} else {
			recorder = mRecorders.get(cacheKey);
			if (recorder == null) {
				synchronized (mLock) {
					recorder = mRecorders.get(cacheKey);
					if (recorder == null) {
						recorder = new Recorder();
						mRecorders.put(cacheKey, recorder);
					}
				}
			}
		}
		return recorder;
	}

	public void endResolve(String host, RequestIpType type, String cacheKey) {
		Recorder recorder = getRecorder(cacheKey);
		recorder.endResolve(host, type);
	}

	public boolean await(String host, RequestIpType type, String cacheKey, long timeout,
						 TimeUnit unit) throws InterruptedException {
		Recorder recorder = getRecorder(cacheKey);
		return recorder.await(host, type, timeout, unit);
	}
}
