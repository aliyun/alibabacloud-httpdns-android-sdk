package com.alibaba.sdk.android.httpdns;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsInstanceHolder;
import com.alibaba.sdk.android.httpdns.impl.InstanceCreator;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * Httpdns实例管理
 */
public class HttpDns {

    private static HttpDnsInstanceHolder sHolder = new HttpDnsInstanceHolder(new InstanceCreator());

    /**
     * 获取HttpDnsService对象
     *
     * @param applicationContext 当前APP的Context
     * @param accountID          HttpDns控制台分配的AccountID
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext, final String accountID) {
        return sHolder.get(applicationContext, accountID, null);
    }

    /**
     * 获取HttpDnsService对象，并启用鉴权功能
     *
     * @param applicationContext 当前APP的Context
     * @param accountID          HttpDns控制台分配的AccountID
     * @param secretKey          用户鉴权私钥
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext, final String accountID, final String secretKey) {
        return sHolder.get(applicationContext, accountID, secretKey);
    }

    /**
     * 获取HttpDnsService对象，初始化时不传入任何参数，靠统一接入服务获取相关参数
     *
     * @param applicationContext 当前APP的Context
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext) {
        return sHolder.get(applicationContext, CommonUtil.getAccountId(applicationContext), CommonUtil.getSecretKey(applicationContext));
    }

    /**
     * 启用或者禁用httpdns，理论上这个是内部接口，不给外部使用的
     * 但是已经对外暴露，所以保留
     *
     * @param enabled
     * @deprecated 启用禁用应该调用实例的方法，而不是控制全部实例的方法
     */
    @Deprecated
    public synchronized static void switchDnsService(boolean enabled) {
        // do nothing as deprecated
    }

    /**
     * 重置httpdns，用于一些模拟应用重启的场景
     */
    public static void resetInstance() {
        sHolder = new HttpDnsInstanceHolder(new InstanceCreator());
        NetworkStateManager.getInstance().reset();
    }
}
