package com.alibaba.sdk.android.httpdns.ranking;

/**
 * IP优选的结果回调
 */
public interface IPRankingCallback {
	void onResult(String host, String[] sortedIps);
}
