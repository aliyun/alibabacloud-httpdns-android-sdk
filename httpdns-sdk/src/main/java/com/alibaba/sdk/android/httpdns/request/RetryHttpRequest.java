package com.alibaba.sdk.android.httpdns.request;

/**
 * 失败时 重试请求
 */
public class RetryHttpRequest<T> extends HttpRequestWrapper<T> {

	private int retryCount;

	public RetryHttpRequest(HttpRequest<T> request, int retryCount) {
		super(request);
		this.retryCount = retryCount;
	}

	@Override
	public T request() throws Throwable {
		while (true) {
			try {
				return super.request();
			} catch (Throwable throwable) {
				if (shouldRetry(throwable)) {
					if (retryCount > 0) {
						retryCount--;
					} else {
						throw throwable;
					}
				} else {
					throw throwable;
				}
			}
		}
	}

	private boolean shouldRetry(Throwable throwable) {
		if (throwable instanceof HttpException) {
			return ((HttpException)throwable).shouldRetry();
		} else {
			// 其它异常都可以重试
			return true;
		}
	}
}
