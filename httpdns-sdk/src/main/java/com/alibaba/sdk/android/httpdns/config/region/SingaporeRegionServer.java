package com.alibaba.sdk.android.httpdns.config.region;

import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.utils.Constants;

public class SingaporeRegionServer {
    private static final String[] SERVER_IPS = new String[] {
            "161.117.200.122",
            "47.74.222.190"
    };
    private static final int[] PORTS = null;
    private static final String[] IPV6_SERVER_IPS = new String[] {
            "240b:4000:f10::178",
            "240b:4000:f10::188"
    };
    private static final int[] IPV6_PORTS = null;

    private static final String[] UPDATE_SERVER = new String[] {
            "resolvers-sg.httpdns.aliyuncs.com"
    };
    private static final String[] IPV6_UPDATE_SERVER = new String[] {
            "resolvers-sg.httpdns.aliyuncs.com"
    };

    public static RegionServer getInitServer() {
        return new RegionServer(SERVER_IPS, PORTS, IPV6_SERVER_IPS, IPV6_PORTS, Constants.REGION_SG);
    }

    public static RegionServer getUpdateServer() {
        return new RegionServer(UPDATE_SERVER, Constants.NO_PORTS, IPV6_UPDATE_SERVER, Constants.NO_PORTS, Constants.REGION_SG);
    }
}
