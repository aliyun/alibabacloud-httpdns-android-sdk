package com.alibaba.sdk.android.httpdns.config;

import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 辅助配置的缓存写入和读取
 */
public class ConfigCacheHelper {

	private final AtomicBoolean mSaving = new AtomicBoolean(false);

	public void restoreFromCache(Context context, HttpDnsConfig config) {
		SharedPreferences sp = context.getSharedPreferences(
			Constants.CONFIG_CACHE_PREFIX + config.getAccountId(), Context.MODE_PRIVATE);
		SpCacheItem[] items = config.getCacheItem();
		for (SpCacheItem item : items) {
			item.restoreFromCache(sp);
		}
	}

	public void saveConfigToCache(Context context, HttpDnsConfig config) {
		if (mSaving.compareAndSet(false, true)) {
			try {
				config.getWorker().execute(new WriteCacheTask(context, config, this));
			} catch (Exception ignored) {
				mSaving.set(false);
			}
		}
	}

	static class WriteCacheTask implements Runnable {

		private final Context mContext;
		private final HttpDnsConfig mHttpDnsConfig;
		private final ConfigCacheHelper mCacheHelper;

		public WriteCacheTask(Context context, HttpDnsConfig config, ConfigCacheHelper helper) {
			this.mContext = context;
			this.mHttpDnsConfig = config;
			this.mCacheHelper = helper;
		}

		@Override
		public void run() {
			mCacheHelper.mSaving.set(false);

			SharedPreferences.Editor editor = mContext.getSharedPreferences(
					Constants.CONFIG_CACHE_PREFIX + mHttpDnsConfig.getAccountId(),
					Context.MODE_PRIVATE)
				.edit();
			SpCacheItem[] items = mHttpDnsConfig.getCacheItem();
			for (SpCacheItem item : items) {
				item.saveToCache(editor);
			}
			// 虽然提示建议使用apply，但是实践证明，apply是把写文件操作推迟到了一些界面切换等时机，反而影响了UI线程。不如直接在子线程写文件
			editor.commit();
		}
	}
}
