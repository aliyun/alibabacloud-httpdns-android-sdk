package com.alibaba.sdk.android.httpdns.serverip;

import java.util.HashMap;

public class UpdateRegionServerLocker {

	private final HashMap<String, Long> mCache = new HashMap<>();
	private final int mTimeout;

	private final Object mLock = new Object();

	public UpdateRegionServerLocker(int timeout) {
		this.mTimeout = timeout;
	}

	public UpdateRegionServerLocker() {
		this.mTimeout = 5 * 60 * 1000;
	}

	public boolean begin(String region) {
		Long requestTime = mCache.get(region);
		if (requestTime != null) {

			if (System.currentTimeMillis() - requestTime > mTimeout) {
				// 超过五分钟了，还没有end。
				// 主动end
				end(region);
			}
			return false;
		} else {
			synchronized (mLock) {
				requestTime = mCache.get(region);
				if (requestTime != null) {
					return false;
				} else {
					mCache.put(region, System.currentTimeMillis());
					return true;
				}
			}
		}
	}

	public void end(String region) {
		mCache.remove(region);
	}
}
