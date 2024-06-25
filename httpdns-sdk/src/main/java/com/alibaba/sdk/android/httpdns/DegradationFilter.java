package com.alibaba.sdk.android.httpdns;

/**
 * 降级判断开关接口
 */
@Deprecated
public interface DegradationFilter {
    /**
     * 是否应该不使用httpdns
     *
     */
    boolean shouldDegradeHttpDNS(String hostName);
}
