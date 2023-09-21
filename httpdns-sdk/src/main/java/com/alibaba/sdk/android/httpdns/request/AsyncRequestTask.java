package com.alibaba.sdk.android.httpdns.request;

/**
 * 数据请求转异步
 */
public abstract class AsyncRequestTask<T> implements Runnable {

	private final RequestCallback<T> mCallback;

	public AsyncRequestTask(RequestCallback<T> callback) {
		this.mCallback = callback;
	}

	/**
	 * 请求数据，不需要直接调用，除非想要同步获取请求数据
	 */
	public abstract T request() throws Throwable;

	@Override
	public void run() {
		try {
			T t = request();
			mCallback.onSuccess(t);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			mCallback.onFail(throwable);
		}
	}
}
