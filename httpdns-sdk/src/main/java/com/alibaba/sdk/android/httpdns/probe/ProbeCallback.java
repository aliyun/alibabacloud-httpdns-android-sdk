package com.alibaba.sdk.android.httpdns.probe;

/**
 * IP优选的结果回调
 */
public interface ProbeCallback {
	void onResult(String host, String[] sortedIps);
}
