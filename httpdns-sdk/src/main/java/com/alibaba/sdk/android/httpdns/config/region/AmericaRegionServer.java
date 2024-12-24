package com.alibaba.sdk.android.httpdns.config.region;

import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.utils.Constants;

public class AmericaRegionServer {
    private static final String [] SERVER_IPS = new String [] {
            "47.246.131.175",
            "47.246.131.141"
    };
    private static final int[] PORTS = null;
    private static final String[] IPV6_SERVER_IPS = new String [] {
            "2404:2280:4000::2bb",
            "2404:2280:4000::23e"
    };
    private static final int[] IPV6_PORTS = null;

    private static final String[] UPDATE_SERVER = new String[] {
            "resolvers-us.httpdns.aliyuncs.com"
    };
    private static final String[] IPV6_UPDATE_SERVER = new String[] {
            "resolvers-us.httpdns.aliyuncs.com"
    };

    public static RegionServer getInitServer() {
        return new RegionServer(SERVER_IPS, PORTS, IPV6_SERVER_IPS, IPV6_PORTS, Constants.REGION_US);
    }

    public static RegionServer getUpdateServer() {
        return new RegionServer(UPDATE_SERVER, Constants.NO_PORTS, IPV6_UPDATE_SERVER, Constants.NO_PORTS, Constants.REGION_US);
    }
}
