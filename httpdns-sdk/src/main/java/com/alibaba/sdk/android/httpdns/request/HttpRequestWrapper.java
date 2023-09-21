package com.alibaba.sdk.android.httpdns.request;

/**
 * 网络请求的包装类
 */
public class HttpRequestWrapper<T> extends HttpRequest<T> {

	private final HttpRequest<T> mHttpRequest;

	public HttpRequestWrapper(HttpRequest<T> request) {
		this.mHttpRequest = request;
	}

	@Override
	public HttpRequestConfig getRequestConfig() {
		return mHttpRequest.getRequestConfig();
	}

	@Override
	public T request() throws Throwable {
		return mHttpRequest.request();
	}
}
