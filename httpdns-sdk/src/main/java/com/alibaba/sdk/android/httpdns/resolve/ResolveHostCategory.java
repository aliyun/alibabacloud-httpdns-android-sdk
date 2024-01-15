package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;

/**
 * 域名解析策略接口
 */
public interface ResolveHostCategory {
	/**
	 * 解析域名
	 */
	void resolve(HttpDnsConfig config, HttpRequestConfig requestConfig,
				 RequestCallback<ResolveHostResponse> callback);
}
