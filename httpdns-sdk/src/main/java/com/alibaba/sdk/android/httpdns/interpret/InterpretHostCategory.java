package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;

/**
 * 域名解析策略接口
 */
public interface InterpretHostCategory {
	/**
	 * 解析域名
	 */
	void interpret(HttpDnsConfig config, HttpRequestConfig requestConfig,
				   RequestCallback<InterpretHostResponse> callback);
}
