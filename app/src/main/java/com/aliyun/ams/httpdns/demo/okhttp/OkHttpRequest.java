package com.aliyun.ams.httpdns.demo.okhttp;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.aliyun.ams.httpdns.demo.MyApp;
import com.aliyun.ams.httpdns.demo.NetworkRequest;
import com.aliyun.ams.httpdns.demo.utils.Util;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * okhttp实现网络请求
 *
 * @author zonglin.nzl
 * @date 8/31/22
 */
public class OkHttpRequest implements NetworkRequest {

    public static final String TAG = MyApp.TAG + "Okhttp";
    private OkHttpClient client;

    private boolean async;
    private RequestIpType type;

    public OkHttpRequest(final Context context) {
        client = new OkHttpClient.Builder()
                // 这里配置连接池，是为了方便测试httpdns能力，正式代码请不要配置
                .connectionPool(new ConnectionPool(0, 10 * 1000, TimeUnit.MICROSECONDS))
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        HTTPDNSResult result;
                        if (async) {
                            result = MyApp.getInstance().getService().getIpsByHostAsync(hostname, type);
                        } else {
                            result = ((SyncService) MyApp.getInstance().getService()).getByHost(hostname, type);
                        }
                        Log.d(TAG, "httpdns 解析 " + hostname + " 结果为 " + result + " ttl is " + Util.getTtl(result));
                        List<InetAddress> inetAddresses = new ArrayList<>();
                        // 这里需要根据实际情况选择使用ipv6地址 还是 ipv4地址， 下面示例的代码优先使用了ipv6地址
                        if (result.getIpv6s() != null && result.getIpv6s().length > 0 && HttpDnsNetworkDetector.getInstance().getNetType(context) != NetType.v4) {
                            for (int i = 0; i < result.getIpv6s().length; i++) {
                                inetAddresses.addAll(Arrays.asList(InetAddress.getAllByName(result.getIpv6s()[i])));
                            }
                            Log.d(TAG, "使用ipv6地址" + inetAddresses);
                        } else if (result.getIps() != null && result.getIps().length > 0 && HttpDnsNetworkDetector.getInstance().getNetType(context) != NetType.v6) {
                            for (int i = 0; i < result.getIps().length; i++) {
                                inetAddresses.addAll(Arrays.asList(InetAddress.getAllByName(result.getIps()[i])));
                            }
                            Log.d(TAG, "使用ipv4地址" + inetAddresses);
                        }
                        if (inetAddresses.size() == 0) {
                            Log.d(TAG, "httpdns 未返回IP，走localdns");
                            return Dns.SYSTEM.lookup(hostname);
                        }
                        return inetAddresses;
                    }
                })
                .build();
    }

    @Override
    public void updateHttpDnsConfig(boolean async, RequestIpType requestIpType) {
        this.async = async;
        this.type = requestIpType;
    }

    @Override
    public String httpGet(String url) throws Exception {
        Log.d(TAG, "使用okhttp 请求" + url + " 异步接口 " + async + " ip类型 " + type.name());
        Response response = client.newCall(new Request.Builder().url(url).build()).execute();
        int code = response.code();
        String body = response.body().string();
        Log.d(TAG, "使用okhttp 请求结果 code " + code + " body " + body);
        if (code != HttpURLConnection.HTTP_OK) {
            throw new Exception("请求失败 code " + code + " body " + body);
        }
        return body;
    }
}
