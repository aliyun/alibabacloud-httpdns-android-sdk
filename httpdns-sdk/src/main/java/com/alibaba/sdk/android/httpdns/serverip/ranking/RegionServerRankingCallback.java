package com.alibaba.sdk.android.httpdns.serverip.ranking;

public interface RegionServerRankingCallback {
    void onResult(String[] sortedIps, int[] ports);
}
