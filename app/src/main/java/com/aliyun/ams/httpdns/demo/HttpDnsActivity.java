package com.aliyun.ams.httpdns.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.aliyun.ams.httpdns.demo.base.BaseActivity;
import com.aliyun.ams.httpdns.demo.http.HttpUrlConnectionRequest;
import com.aliyun.ams.httpdns.demo.okhttp.OkHttpRequest;
import com.aliyun.ams.httpdns.demo.utils.ThreadUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zonglin.nzl
 * @date 8/30/22
 */
public class HttpDnsActivity extends BaseActivity {

    public static final String SCHEMA_HTTPS = "https://";
    public static final String SCHEMA_HTTP = "http://";

    public static final String APPLE_URL = "www.apple.com";
    public static final String TAOBAO_URL = "www.taobao.com";
    public static final String ALICDN_URL = "gw.alicdn.com";
    public static final String ALIYUN_URL = "www.aliyun.com";
    public static final String IPV6TEST = "ipv6.sjtu.edu.cn";

    public static final String[] hosts = new String[]{
            APPLE_URL, TAOBAO_URL, ALICDN_URL, ALIYUN_URL, IPV6TEST
    };

    public static String getUrl(String schema, String host) {
        if (host.equals(ALICDN_URL)) {
            return schema + host + "/imgextra/i4/O1CN01QmAvnA1IgJvX1GfLm_!!6000000000922-2-tps-176-176.png";
        }
        return schema + host;
    }

    /**
     * 要请求的schema
     */
    private String schema = SCHEMA_HTTPS;
    /**
     * 要请求的域名
     */
    private String host = APPLE_URL;
    /**
     * 要解析的ip类型
     */
    private RequestIpType requestIpType = RequestIpType.v4;

    private HttpUrlConnectionRequest httpUrlConnectionRequest;
    private OkHttpRequest okHttpRequest;
    private NetworkRequest networkRequest = httpUrlConnectionRequest;

    private ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpUrlConnectionRequest = new HttpUrlConnectionRequest(this);
        okHttpRequest = new OkHttpRequest(this);
        networkRequest = httpUrlConnectionRequest;

        addFourButton("切换实例", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().changeHolder();
                sendLog("httpdns实例已切换");
            }
        }, "获取配置", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLog(MyApp.getInstance().getCurrentHolder().getCurrentConfig());
                sendLog("要解析的域名是" + host);
                sendLog("要解析的ip类型是" + requestIpType.name());
                sendLog("要模拟请求的url是" + getUrl(schema, host));
                sendLog("模拟请求的网络框架是" + (networkRequest == httpUrlConnectionRequest ? " HttpUrlConnection" : "OkHttp"));
            }
        }, "清除配置缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().cleanSp();
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "配置缓存清除, 重启生效");
            }
        }, "清除日志", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanLog();
            }
        });

        addTwoButton("开启https", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableHttps(true);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "开启https");
            }
        }, "关闭https", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableHttps(false);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "关闭https");
            }
        });

        addTwoButton("允许过期IP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableExpiredIp(true);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "允许过期IP");
            }
        }, "不允许过期IP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableExpiredIp(false);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "不允许过期IP");
            }
        });


        addTwoButton("允许持久化缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableCacheIp(true);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "允许持久化缓存");
            }
        }, "不允许持久化缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setEnableCacheIp(false);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "不允许持久化缓存");
            }
        });

        addThreeButton("设置中国大陆", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setRegion(null);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "切换到中国大陆");
            }
        }, "设置中国香港", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setRegion("hk");
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "切换到中国香港");
            }
        }, "设置新加坡", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getCurrentHolder().setRegion("sg");
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "切换到新加坡");
            }
        });

        addEditTextButton("超时时长 ms", "设置超时ms", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                EditText et = (EditText) view;
                int timeout = Integer.parseInt(et.getEditableText().toString());
                MyApp.getInstance().getCurrentHolder().setTimeout(timeout);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "设置超时 " + timeout);
            }
        });

        addTwoButton("开启降级", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getService().setDegradationFilter(new DegradationFilter() {
                    @Override
                    public boolean shouldDegradeHttpDNS(String hostName) {
                        // 此处可以根据域名判断是否不使用httpdns， true 不使用， false 使用。
                        return true;
                    }
                });
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "开启降级");
            }
        }, "关闭降级", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getService().setDegradationFilter(null);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "关闭降级");
            }
        });

        addTwoButton("开启网络变化后预解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getService().setPreResolveAfterNetworkChanged(true);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "开启网络变化后预解析");
            }
        }, "关闭网络变化后预解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getService().setPreResolveAfterNetworkChanged(false);
                sendLog(MyApp.getInstance().getCurrentHolder().getAccountId() + "关闭网络变化后预解析");
            }
        });

        addView(R.layout.item_autocomplete_edittext_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {
                final AutoCompleteTextView actvOne = view.findViewById(R.id.actvOne);
                final EditText etOne = view.findViewById(R.id.etOne);
                Button btnOne = view.findViewById(R.id.btnOne);

                actvOne.setHint("请输入域名");
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, hosts);
                actvOne.setAdapter(adapter);

                etOne.setHint("请输入自定义ttl");

                btnOne.setText("指定域名ttl s");
                btnOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String host = actvOne.getEditableText().toString();
                        int ttl = Integer.parseInt(etOne.getEditableText().toString());
                        MyApp.getInstance().getCurrentHolder().setHostTtl(host, ttl);
                        sendLog("指定域名" + host + "的ttl为" + ttl + "秒");
                    }
                });
            }
        });

        addView(R.layout.item_autocomplete_edittext_button, new OnViewCreated() {
            @Override
            public void onViewCreated(View view) {
                final AutoCompleteTextView actvOne = view.findViewById(R.id.actvOne);
                final EditText etOne = view.findViewById(R.id.etOne);
                Button btnOne = view.findViewById(R.id.btnOne);

                actvOne.setHint("域名");
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, hosts);
                actvOne.setAdapter(adapter);

                etOne.setHint("请输入端口");

                btnOne.setText("添加probe配置");
                btnOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String host = actvOne.getEditableText().toString();
                        int port = Integer.parseInt(etOne.getEditableText().toString());
                        MyApp.getInstance().getCurrentHolder().addIpProbeItem(new IPProbeItem(host, port));
                        sendLog("添加域名" + host + " 探测");
                    }
                });
            }
        });

        addAutoCompleteTextViewButton(hosts, "主站域名", "添加主站域名", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                AutoCompleteTextView actvOne = (AutoCompleteTextView) view;
                String host = actvOne.getEditableText().toString();
                MyApp.getInstance().getCurrentHolder().addHostWithFixedIp(host);
                sendLog("添加主站域名" + host);
            }
        });

        addAutoCompleteTextViewButton(hosts, "域名", "删除指定域名的缓存", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                AutoCompleteTextView actvOne = (AutoCompleteTextView) view;
                String host = actvOne.getEditableText().toString();
                ArrayList<String> list = new ArrayList<>();
                list.add(host);
                MyApp.getInstance().getService().cleanHostCache(list);
                sendLog("清除指定host的缓存" + host);
            }
        });

        addOneButton("清除所有缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.getInstance().getService().cleanHostCache(null);
                sendLog("清除所有缓存");
            }
        });

        addFourButton("获取当前网络状态", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetType type = HttpDnsNetworkDetector.getInstance().getNetType(getApplicationContext());
                sendLog("获取网络状态 " + type.name());
            }
        }, "禁用网络状态缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpDnsNetworkDetector.getInstance().disableCache(true);
                sendLog("网络状态 禁用缓存 ");
            }
        }, "开启网络状态缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpDnsNetworkDetector.getInstance().disableCache(false);
                sendLog("网络状态 开启缓存 ");
            }
        }, "清除网络状态缓存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpDnsNetworkDetector.getInstance().cleanCache(false);
                sendLog("网络状态清除缓存 ");
            }
        });

        addTwoButton("禁止读取IP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpDnsNetworkDetector.getInstance().setCheckInterface(false);
                sendLog("查询网络状态时 禁止读取IP");
            }
        }, "允许读取IP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpDnsNetworkDetector.getInstance().setCheckInterface(true);
                sendLog("查询网络状态时 允许读取IP");
            }
        });

        addAutoCompleteTextViewButton(hosts, "域名", "设置检测网络使用的域名", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                AutoCompleteTextView actvOne = (AutoCompleteTextView) view;
                String host = actvOne.getEditableText().toString();
                HttpDnsNetworkDetector.getInstance().setHostToCheckNetType(host);
                sendLog("设置检测网络状态使用的域名为" + host);
            }
        });

        addTwoButton("模拟请求使用https请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schema = SCHEMA_HTTPS;
                sendLog("测试url使用https");
            }
        }, "模拟请求使用http请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schema = SCHEMA_HTTP;
                sendLog("测试url使用http");
            }
        });

        addTwoButton("HttpUrlConnection", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                networkRequest = httpUrlConnectionRequest;
                sendLog("指定网络实现方式为HttpUrlConnection");
            }
        }, "Okhttp", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                networkRequest = okHttpRequest;
                sendLog("指定网络实现方式为okhttp");
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

        addAutoCompleteTextViewButton(hosts, "域名", "指定要解析的域名", new OnButtonClick() {
            @Override
            public void onBtnClick(View view) {
                AutoCompleteTextView actvOne = (AutoCompleteTextView) view;
                host = actvOne.getEditableText().toString();
                sendLog("要解析的域名" + host);
            }
        });

        addTwoButton("异步解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                worker.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendLog("开始发起网络请求");
                        sendLog("网络实现方式为" + (networkRequest == httpUrlConnectionRequest ? "HttpUrlConnection" : "okhttp"));
                        String url = getUrl(schema, host);
                        sendLog("url is " + url);
                        sendLog("httpdns 使用 异步解析api");
                        sendLog("指定解析ip类型为" + requestIpType.name());
                        networkRequest.updateHttpDnsConfig(true, requestIpType);
                        try {
                            String response = networkRequest.httpGet(url);
                            if (response != null && response.length() > 30) {
                                response = response.trim();
                                if (response.length() > 30) {
                                    response = response.substring(0, 30) + "...";
                                }
                            }
                            sendLog("请求结束 response is " + response + " 完整记录请看logcat日志");
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendLog("请求结束 发生异常 " + e.getClass().getName() + e.getMessage() + " 完整记录请看logcat日志");
                        }

                    }
                });
            }
        }, "同步解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                worker.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendLog("开始发起网络请求");
                        sendLog("网络实现方式为" + (networkRequest == httpUrlConnectionRequest ? "HttpUrlConnection" : "okhttp"));
                        String url = getUrl(schema, host);
                        sendLog("url is " + url);
                        sendLog("httpdns 使用 同步解析api");
                        sendLog("指定解析ip类型为" + requestIpType.name());
                        networkRequest.updateHttpDnsConfig(false, requestIpType);
                        try {
                            String response = networkRequest.httpGet(url);
                            if (response != null && response.length() > 30) {
                                response = response.substring(0, 30) + "...";
                            }
                            sendLog("请求结束 response is " + response + " 完整记录请看logcat日志");
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendLog("请求结束 发生异常 " + e.getClass().getName() + e.getMessage() + " 完整记录请看logcat日志");
                        }
                    }
                });
            }
        });

        addThreeButton("发起预解析", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                worker.execute(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> tmp = new ArrayList<>();
                        for (int i = 0; i < hosts.length; i++) {
                            tmp.add(hosts[i]);
                        }
                        MyApp.getInstance().getService().setPreResolveHosts(tmp, requestIpType);
                        sendLog("已发起预解析请求");
                    }
                });
            }
        }, "跳转SDNS测试界面", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HttpDnsActivity.this, SDNSActivity.class));
            }
        }, "跳转Webview测试界面", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HttpDnsActivity.this, WebViewActivity.class));
            }
        });


        final String[] validHosts = new String[]{
                "www.aliyun.com",
                "www.taobao.com",
                "www.google.com",
                "api.vpgame.com",
                "yqh.aliyun.com",
                "www.test1.com",
                "ib.snssdk.com",
                "www.baidu.com",
                "www.dddzzz.com",
                "isub.snssdk.com",
                "www.facebook.com",
                "m.vpgame.com",
                "img.momocdn.com",
                "ipv6.l.google.com",
                "ipv6.sjtu.edu.cn",
                "fdfs.xmcdn.com",
                "testcenter.dacangjp.com",
                "mobilegw.alipaydns.com",
                "ww1.sinaimg.cn.w.alikunlun.com",
                "x2ipv6.tcdn.qq.com",
                "hiphotos.jomodns.com",
                "suning.xdwscache.ourwebcdn.com",
                "gw.alicdn.com",
                "dou.bz",
                "www.apple.com",
        };

        addTwoButton("并发异步请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThreadUtil.multiThreadTest(validHosts, 100, 20, 10 * 60 * 1000, true, requestIpType);
                sendLog("异步api并发测试开始，大约耗时10分钟，请最后查看logcat日志，确认结果，建议关闭httpdns日志，避免日志量过大");
            }
        }, "并发同步请求", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThreadUtil.multiThreadTest(validHosts, 100, 20, 10 * 60 * 1000, false, requestIpType);
                sendLog("同步api并发测试开始，大约耗时10分钟，请最后查看logcat日志，确认结果，建议关闭httpdns日志，避免日志量过大");
            }
        });

    }
}
