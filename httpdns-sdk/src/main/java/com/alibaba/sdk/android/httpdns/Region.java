package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.utils.Constants;

public enum Region {
    DEFAULT(""), HK(Constants.REGION_HK), SG(Constants.REGION_SG), DE(Constants.REGION_DE),
    US(Constants.REGION_US);

    private final String region;
    Region(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }
}
