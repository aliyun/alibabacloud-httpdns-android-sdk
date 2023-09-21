package com.alibaba.sdk.android.httpdns.request;

/**
 * http请求结果回调
 */
public interface RequestCallback<T> {
	void onSuccess(T response);

	void onFail(Throwable throwable);
}
