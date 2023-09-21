package com.aliyun.ams.httpdns.demo.utils;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;

import java.lang.reflect.Field;

/**
 * @author zonglin.nzl
 * @date 9/8/22
 */
public class Util {


    /**
     * 获取ttl，
     * 此方法是用于测试自定义ttl是否生效
     *
     * @param result
     * @return
     */
    public static int getTtl(HTTPDNSResult result) {
        try {
            Field ttlField = HTTPDNSResult.class.getDeclaredField("ttl");
            ttlField.setAccessible(true);
            return ttlField.getInt(result);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
