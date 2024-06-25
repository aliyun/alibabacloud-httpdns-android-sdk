package com.alibaba.sdk.android.httpdns;

/**
 * 不使用HttpDns的配置接口
 */
public interface NotUseHttpDnsFilter {
    /**
     * 是否应该不使用httpdns
     * @param hostName 域名
     * @return true 不走httpdns解析 ｜ false 走httpdns解析
     *
     */
    boolean notUseHttpDns(String hostName);
}
