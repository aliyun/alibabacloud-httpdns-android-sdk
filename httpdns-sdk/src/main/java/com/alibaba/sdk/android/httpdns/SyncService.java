package com.alibaba.sdk.android.httpdns;

public interface SyncService {
	/**
	 * 同步解析接口，必须在子线程中执行，否则没有效果
	 *
	 * @deprecated 该接口已废弃，后续版本可能会删除，请使用
	 * {@link HttpDnsService#getHttpDnsResultForHostSync(String, RequestIpType)}
	 */
	@Deprecated
	HTTPDNSResult getByHost(String host, RequestIpType type);

}
