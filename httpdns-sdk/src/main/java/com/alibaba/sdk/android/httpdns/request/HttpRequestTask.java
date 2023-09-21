package com.alibaba.sdk.android.httpdns.request;

/**
 * 网络请求的异步任务
 */
public class HttpRequestTask<T> extends AsyncRequestTask<T> {

	private final HttpRequest<T> mHttpRequest;

	public HttpRequestTask(HttpRequest<T> httpRequest, RequestCallback<T> callback) {
		super(callback);
		this.mHttpRequest = httpRequest;
	}

	@Override
	public T request() throws Throwable {
		return mHttpRequest.request();
	}
}
