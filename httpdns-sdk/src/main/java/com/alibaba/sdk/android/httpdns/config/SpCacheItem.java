package com.alibaba.sdk.android.httpdns.config;

import android.content.SharedPreferences;

public interface SpCacheItem {
	void restoreFromCache(SharedPreferences sp);

	void saveToCache(SharedPreferences.Editor editor);
}
