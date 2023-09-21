package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.probe.ProbeTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public interface ApiForTest {

    /**
     * 指定初始服务ip
     *  @param region
     * @param ips
     * @param ports
     * @param ipv6s
     * @param v6Ports
     */
    void setInitServer(String region, String[] ips, int[] ports, String[] ipv6s, int[] v6Ports);

    /**
     * 指定httpdns使用的线程池
     * @param scheduledExecutorService
     */
    void setThread(ScheduledExecutorService scheduledExecutorService);

    /**
     * 指定 测试使用的socket factory
     * @param speedTestSocketFactory
     */
    void setSocketFactory(ProbeTask.SpeedTestSocketFactory speedTestSocketFactory);

    /**
     * 指定调度接口的调用间歇，避免正常的间歇过长无法测试
     * @param timeInterval
     */
    void setUpdateServerTimeInterval(int timeInterval);

    /**
     * 指定 sniff模式的 请求间歇
     * @param timeInterval
     */
    void setSniffTimeInterval(int timeInterval);

    /**
     * 获取httpdns的线程池用于控制异常操作
     *
     * @return
     */
    ExecutorService getWorker();

    /**
     * 设置兜底的调度ip
     *
     * @param defaultServerIps
     * @param ports
     */
    void setDefaultUpdateServer(String[] defaultServerIps, int[] ports);

    /**
     * 设置ipv6的兜底调度IP
     *
     * @param defaultServerIps
     * @param ports
     */
    void setDefaultUpdateServerIpv6(String[] defaultServerIps, int[] ports);

    /**
     * 设置测试用的网络detector
     *
     * @param networkDetector
     */
    void setNetworkDetector(HttpDnsSettings.NetworkDetector networkDetector);
}
