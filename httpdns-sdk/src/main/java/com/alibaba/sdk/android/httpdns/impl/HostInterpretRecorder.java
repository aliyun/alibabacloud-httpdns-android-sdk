package com.alibaba.sdk.android.httpdns.impl;

import java.util.HashMap;
import java.util.HashSet;

import com.alibaba.sdk.android.httpdns.RequestIpType;

public class HostInterpretRecorder {

	private static class Recorder {
		private final HashSet<String> mV4ResolvingHost = new HashSet<>();
		private final HashSet<String> mV6ResolvingHost = new HashSet<>();
		private final HashSet<String> mBothResolvingHost = new HashSet<>();
		private final Object mLock = new Object();

		public boolean beginInterpret(String host, RequestIpType type) {

			if (type == RequestIpType.both) {
				if (mBothResolvingHost.contains(host) || (mV4ResolvingHost.contains(host)
					&& mV6ResolvingHost.contains(host))) {
					// 正在解析
					return false;
				} else {
					synchronized (mLock) {
						if (mBothResolvingHost.contains(host) || (mV4ResolvingHost.contains(host)
							&& mV6ResolvingHost.contains(host))) {
							// 正在解析
							return false;
						} else {
							mBothResolvingHost.add(host);
							return true;
						}
					}
				}
			} else if (type == RequestIpType.v4) {
				if (mV4ResolvingHost.contains(host) || mBothResolvingHost.contains(host)) {
					return false;
				} else {
					synchronized (mLock) {
						if (mV4ResolvingHost.contains(host) || mBothResolvingHost.contains(host)) {
							return false;
						} else {
							mV4ResolvingHost.add(host);
							return true;
						}
					}
				}
			} else if (type == RequestIpType.v6) {
				if (mV6ResolvingHost.contains(host) || mBothResolvingHost.contains(host)) {
					return false;
				} else {
					synchronized (mLock) {
						if (mV6ResolvingHost.contains(host) || mBothResolvingHost.contains(host)) {
							return false;
						} else {
							mV6ResolvingHost.add(host);
							return true;
						}
					}
				}
			}
			return false;
		}

		public void endInterpret(String host, RequestIpType type) {
			switch (type) {
				case v4:
					mV4ResolvingHost.remove(host);
					break;
				case v6:
					mV6ResolvingHost.remove(host);
					break;
				case both:
					mBothResolvingHost.remove(host);
					break;
			}
		}
	}

	private final Object mLock = new Object();
	private final Recorder mDefaultRecorder = new Recorder();
	private final HashMap<String, Recorder> mRecorderHashMap = new HashMap<>();

	public boolean beginInterpret(String host, RequestIpType type) {
		return beginInterpret(host, type, null);
	}

	public void endInterpret(String host, RequestIpType type) {
		endInterpret(host, type, null);
	}

	public boolean beginInterpret(String host, RequestIpType type, String cacheKey) {
		Recorder recorder = getRecorder(cacheKey);
		return recorder.beginInterpret(host, type);
	}

	private Recorder getRecorder(String cacheKey) {
		Recorder recorder = null;
		if (cacheKey == null || cacheKey.isEmpty()) {
			recorder = mDefaultRecorder;
		} else {
			recorder = mRecorderHashMap.get(cacheKey);
			if (recorder == null) {
				synchronized (mLock) {
					recorder = mRecorderHashMap.get(cacheKey);
					if (recorder == null) {
						recorder = new Recorder();
						mRecorderHashMap.put(cacheKey, recorder);
					}
				}
			}
		}
		return recorder;
	}

	public void endInterpret(String host, RequestIpType type, String cacheKey) {
		Recorder recorder = getRecorder(cacheKey);
		recorder.endInterpret(host, type);
	}
}
