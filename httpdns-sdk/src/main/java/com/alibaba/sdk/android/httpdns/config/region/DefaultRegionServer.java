package com.alibaba.sdk.android.httpdns.config.region;

import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.utils.Constants;

public final class DefaultRegionServer {
    private static final String [] SERVER_IPS = new String [] {
            "203.107.1.1",
            "203.107.1.97",
            "203.107.1.100",
            "203.119.238.240",
            "106.11.25.239",
            "59.82.99.47"
    };
    private static final int[] PORTS = null;
    private static final String[] IPV6_SERVER_IPS = new String [] {
            "2401:b180:7001::31d",
            "2408:4003:1f40::30a",
            "2401:b180:2000:20::10",
            "2401:b180:2000:30::1c"
    };
    private static final int[] IPV6_PORTS = null;

    private static final String[] UPDATE_SERVER = new String[] {
            "resolvers-cn.httpdns.aliyuncs.com"
    };
    private static final String[] IPV6_UPDATE_SERVER = new String[] {
            "resolvers-cn.httpdns.aliyuncs.com"
    };

    public static RegionServer getInitServer() {
        return new RegionServer(SERVER_IPS, PORTS, IPV6_SERVER_IPS, IPV6_PORTS, Constants.REGION_DEFAULT);
    }

    public static RegionServer getUpdateServer() {
        return new RegionServer(UPDATE_SERVER, Constants.NO_PORTS, IPV6_UPDATE_SERVER, Constants.NO_PORTS, Constants.REGION_DEFAULT);
    }
}
