package com.alibaba.sdk.android.httpdns.config.region;

import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.Constants;

public class RegionServerManager {
    public static RegionServer getInitServer(String region) {
        RegionServer regionServer = null;
        switch (region) {
            case Constants.REGION_HK:
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("use hk region");
                }
                regionServer = HongKongRegionServer.getInitServer();
                break;
            case Constants.REGION_SG:
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("use sg region");
                }
                regionServer = SingaporeRegionServer.getInitServer();
                break;
            case Constants.REGION_DE:
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("use de region");
                }
                regionServer = GermanyRegionServer.getInitServer();
                break;
            case Constants.REGION_US:
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("use us region");
                }
                regionServer = AmericaRegionServer.getInitServer();
                break;
            default:
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("use default region");
                }
                regionServer = DefaultRegionServer.getInitServer();
                break;
        }

        return regionServer;
    }

    public static RegionServer getUpdateServer(String region) {
        RegionServer regionServer = null;
        switch (region) {
            case Constants.REGION_HK:
                regionServer = HongKongRegionServer.getUpdateServer();
                break;
            case Constants.REGION_SG:
                regionServer = SingaporeRegionServer.getUpdateServer();
                break;
            case Constants.REGION_DE:
                regionServer = GermanyRegionServer.getUpdateServer();
                break;
            case Constants.REGION_US:
                regionServer = AmericaRegionServer.getUpdateServer();
                break;
            default:
                regionServer = DefaultRegionServer.getUpdateServer();
        }

        return regionServer;
    }
}
