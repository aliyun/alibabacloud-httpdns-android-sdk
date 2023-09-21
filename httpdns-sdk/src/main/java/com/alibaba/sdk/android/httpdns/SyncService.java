package com.alibaba.sdk.android.httpdns;


public interface SyncService {
	/**
	 * 同步解析接口，必须在子线程中执行，否则没有效果
	 */
	HTTPDNSResult getByHost(String host, RequestIpType type);
}
