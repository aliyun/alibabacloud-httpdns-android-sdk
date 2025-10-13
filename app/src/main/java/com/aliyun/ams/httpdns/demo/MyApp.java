package com.aliyun.ams.httpdns.demo;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.aliyun.ams.httpdns.demo.utils.SpUtil;

public class MyApp extends Application {

    private static final String SP_NAME = "HTTPDNS_DEMO";
    private static final String KEY_INSTANCE = "KEY_INSTANCE";
    private static final String VALUE_INSTANCE_A = "A";
    private static final String VALUE_INSTANCE_B = "B";

    public static final String TAG = "HTTPDNS DEMO";
    private static MyApp instance;

    public static MyApp getInstance() {
        return instance;
    }

    private final HttpDnsHolder holderA = new HttpDnsHolder("请替换为测试用A实例的accountId", "请替换为测试用A实例的secret");
    private final HttpDnsHolder holderB = new HttpDnsHolder("请替换为测试用B实例的accountId", null);

    private HttpDnsHolder current = holderA;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 开启logcat 日志 默认关闭, 开发测试过程中可以开启
        HttpDnsLog.enable(true);
        // 注入日志接口，接受httpdns的日志，开发测试过程中可以开启, 基础日志需要先enable才生效，一些错误日志不需要
        HttpDnsLog.setLogger(new ILogger() {
            @Override
            public void log(String msg) {
                Log.d("HttpDnsLogger", msg);
            }
        });

        // 初始化httpdns的配置
        holderA.init(this);
        holderB.init(this);

        SpUtil.readSp(this, SP_NAME, new SpUtil.OnGetSp() {
            @Override
            public void onGetSp(SharedPreferences sp) {
                String flag = sp.getString(KEY_INSTANCE, VALUE_INSTANCE_A);
                if (flag.equals(VALUE_INSTANCE_A)) {
                    current = holderA;
                } else {
                    current = holderB;
                }
            }
        });
    }

    public HttpDnsHolder getCurrentHolder() {
        return current;
    }

    public HttpDnsHolder changeHolder() {
        if (current == holderA) {
            current = holderB;
            SpUtil.writeSp(instance, SP_NAME, new SpUtil.OnGetSpEditor() {
                @Override
                public void onGetSpEditor(SharedPreferences.Editor editor) {
                    editor.putString(KEY_INSTANCE, VALUE_INSTANCE_B);
                }
            });
        } else {
            current = holderA;
            SpUtil.writeSp(instance, SP_NAME, new SpUtil.OnGetSpEditor() {
                @Override
                public void onGetSpEditor(SharedPreferences.Editor editor) {
                    editor.putString(KEY_INSTANCE, VALUE_INSTANCE_A);
                }
            });
        }
        return current;
    }

    public HttpDnsService getService() {
        return current.getService();
    }

}
