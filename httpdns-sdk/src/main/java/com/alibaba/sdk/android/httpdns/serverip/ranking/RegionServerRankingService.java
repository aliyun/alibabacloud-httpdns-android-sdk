package com.alibaba.sdk.android.httpdns.serverip.ranking;

import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.util.Arrays;

public class RegionServerRankingService {
    private HttpDnsConfig mHttpDnsConfig = null;

    public RegionServerRankingService(HttpDnsConfig config) {
        mHttpDnsConfig = config;
    }

    public void rankServiceIp(RegionServer regionServer) {

        //对v4地址进行测速
        if (regionServer.getServerIps() != null && regionServer.getServerIps().length > 1) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("start ranking server ips: " + Arrays.toString(regionServer.getServerIps())
                        + ", ports: " + Arrays.toString(regionServer.getPorts()));
            }

            mHttpDnsConfig.getWorker().execute(new RegionServerRankingTask(mHttpDnsConfig.getSchema(), regionServer.getServerIps(), regionServer.getPorts(), new RegionServerRankingCallback() {
                @Override
                public void onResult(String[] sortedIps, int[] ports) {
                    mHttpDnsConfig.getCurrentServer().updateServerIpv4sRank(sortedIps, ports);
                }
            }));
        }

        //对v6地址进行测速
        if (regionServer.getIpv6ServerIps() != null && regionServer.getIpv6ServerIps().length > 1) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("start ranking server ipv6s: " + Arrays.toString(regionServer.getIpv6ServerIps())
                        + ", ports: " + Arrays.toString(regionServer.getIpv6Ports()));
            }
            mHttpDnsConfig.getWorker().execute(new RegionServerRankingTask(mHttpDnsConfig.getSchema(), regionServer.getIpv6ServerIps(), regionServer.getIpv6Ports(), new RegionServerRankingCallback() {
                @Override
                public void onResult(String[] sortedIps, int[] ports) {
                    mHttpDnsConfig.getCurrentServer().updateServerIpv6sRank(sortedIps, ports);
                }
            }));
        }
    }
}
