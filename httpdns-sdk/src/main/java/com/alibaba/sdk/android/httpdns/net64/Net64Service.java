package com.alibaba.sdk.android.httpdns.net64;

public interface Net64Service {
	///**
	// * 判断当前网络环境是否支持ipv6
	// * @return 支持返回true，否则返回false
	// */
	//    boolean haveIPv6Stack();

	/**
	 * ipv6不再需要开关控制
	 *
	 * @param enable enable为true时开启，否则不开启
	 */
	@Deprecated
	void enableIPv6(boolean enable);

	/**
	 * 获取ipv6地址
	 *
	 * @param host host为目标域名
	 * @return 返回ipv6地址，当enableIPv6未开启时返回null
	 */
	String getIPv6ByHostAsync(String host);
}
