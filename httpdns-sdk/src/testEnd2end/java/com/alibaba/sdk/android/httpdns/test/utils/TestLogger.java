package com.alibaba.sdk.android.httpdns.test.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 测试模块的日志接口
 *
 * @author zonglin.nzl
 * @date 2020/12/18
 */
public class TestLogger {
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

    public static void log(String msg) {
        System.out.println("[" + format.format(new Date()) + "][TEST]" + msg);
    }
}
