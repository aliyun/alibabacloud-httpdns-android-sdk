package com.alibaba.sdk.android.httpdns.resolve;

/**
 * 模式控制接口
 */
public interface StatusControl {
    /**
     * 下调
     */
    void turnDown();

    /**
     * 上调
     */
    void turnUp();
}
