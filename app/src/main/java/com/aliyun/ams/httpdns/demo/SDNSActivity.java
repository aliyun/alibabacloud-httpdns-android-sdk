package com.aliyun.ams.httpdns.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.aliyun.ams.httpdns.demo.base.BaseActivity;

import java.util.HashMap;

import static com.aliyun.ams.httpdns.demo.HttpDnsActivity.APPLE_URL;

/**
 * @author zonglin.nzl
 * @date 8/31/22
 */
public class SDNSActivity extends BaseActivity {

    private HashMap<String, String> globalParams = new HashMap<>();
    /**
     * 要请求的域名
     */
    private String host = APPLE_URL;
    /**
     * 要解析的ip类型
     */
    private RequestIpType requestIpType = RequestIpType.v4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addEditTextEditTextButton("key", "value", "添加全局配置", new OnButtonClickMoreView() {
            @Override
            public void onBtnClick(View[] views) {
                EditText etOne = (EditText) views[0];
                EditText etTwo = (EditText) views[1];

                String key = etOne.getEditableText().toString();
                String value = etTwo.getEditableText().toString();

                globalParams.put(key, value);

                MyApp.getInstance().getService().setSdnsGlobalParams(globalParams);

                sendLog("添加全局参数 " + key + " : " + value);
            }
        });

        addOneButton("清除全局配置", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                globalParams.clear();
                MyApp.getInstance().getService().clearSdnsGlobalParams();
                sendLog("清除全局参数");
            }
        });


        addFourButton("指定v4", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIpType = RequestIpType.v4;
                sendLog("要解析的IP类型指定为ipv4");
            }
        }, "指定v6", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIpType = RequestIpType.v6;
                sendLog("要解析的IP类型指定为ipv6");
            }
        }, "都解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIpType = RequestIpType.both;
                sendLog("要解析的IP类型指定为ipv4和ipv6");
            }
        }, "自动判断", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestIpType = RequestIpType.auto;
                sendLog("要解析的IP类型根据网络情况自动判断");
            }
        });

        addAutoCompleteTextViewButton(HttpDnsActivity.hosts, "域名", "指定要解析的域名", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                AutoCompleteTextView actvOne = (AutoCompleteTextView) view;
                host = actvOne.getEditableText().toString();
                sendLog("要解析的域名" + host);
            }
        });

        addEditTextEditTextButton("key", "value", "发起请求", new OnButtonClickMoreView() {
            @Override
            public void onBtnClick(View[] views) {
                EditText etOne = (EditText) views[0];
                EditText etTwo = (EditText) views[1];

                String key = etOne.getEditableText().toString();
                String value = etTwo.getEditableText().toString();

                HashMap<String, String> map = new HashMap<>();

                map.put(key, value);

                sendLog("发起SDNS请求 requestIpType is " + requestIpType.name());
                HTTPDNSResult result = MyApp.getInstance().getService().getIpsByHostAsync(host, requestIpType, map, "测试SDNS");
                sendLog("结果 " + result);
            }
        });

        addTwoButton("scale1参数请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("scale", "scale1");
                sendLog("发起SDNS请求 requestIpType is " + requestIpType.name() + " scale : scale1");
                HTTPDNSResult result = MyApp.getInstance().getService().getIpsByHostAsync(host, requestIpType, map, "测试1");
                sendLog("结果 " + result);
            }
        }, "scale2参数请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("scale", "scale2");
                sendLog("发起SDNS请求 requestIpType is " + requestIpType.name() + " scale : scale2");
                HTTPDNSResult result = MyApp.getInstance().getService().getIpsByHostAsync(host, requestIpType, map, "测试2");
                sendLog("结果 " + result);
            }
        });

    }
}
