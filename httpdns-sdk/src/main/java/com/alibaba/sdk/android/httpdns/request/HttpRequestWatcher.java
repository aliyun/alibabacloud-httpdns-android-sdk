package com.alibaba.sdk.android.httpdns.request;

/**
 * 监听网络请求，用于附加业务逻辑
 */
public class HttpRequestWatcher<T> extends HttpRequestWrapper<T> {

	private final Watcher mWatcher;

	public HttpRequestWatcher(HttpRequest<T> request, Watcher watcher) {
		super(request);
		this.mWatcher = watcher;
	}

	@Override
	public T request() throws Throwable {
		try {
			if (mWatcher != null) {
				try {
					mWatcher.onStart(getRequestConfig());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			T t = super.request();
			if (mWatcher != null) {
				try {
					mWatcher.onSuccess(getRequestConfig(), t);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return t;
		} catch (Throwable throwable) {

			if (mWatcher != null) {
				try {
					mWatcher.onFail(getRequestConfig(), throwable);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			throw throwable;
		}
	}

	public interface Watcher {
		void onStart(HttpRequestConfig config);

		void onSuccess(HttpRequestConfig config, Object data);

		void onFail(HttpRequestConfig config, Throwable throwable);
	}
}
