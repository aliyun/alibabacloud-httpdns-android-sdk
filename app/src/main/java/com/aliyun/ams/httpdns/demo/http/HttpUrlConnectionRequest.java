package com.aliyun.ams.httpdns.demo.http;


import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;
import com.aliyun.ams.httpdns.demo.MyApp;
import com.aliyun.ams.httpdns.demo.NetworkRequest;
import com.aliyun.ams.httpdns.demo.utils.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * 使用HttpUrlConnection 实现请求
 */
public class HttpUrlConnectionRequest implements NetworkRequest {

    public static final String TAG = MyApp.TAG + "HttpUrl";

    private final Context context;
    private boolean async;
    private RequestIpType type;

    public HttpUrlConnectionRequest(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void updateHttpDnsConfig(boolean async, RequestIpType requestIpType) {
        this.async = async;
        this.type = requestIpType;
    }

    @Override
    public String httpGet(String url) throws Exception {
        Log.d(TAG, "使用httpUrlConnection 请求" + url + " 异步接口 " + async + " ip类型 " + type.name());

        HttpURLConnection conn = getConnection(url);
        InputStream in = null;
        BufferedReader streamReader = null;
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            in = conn.getErrorStream();
            String errStr = null;
            if (in != null) {
                streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                errStr = readStringFrom(streamReader).toString();
            }
            Log.d(TAG, "请求失败 " + conn.getResponseCode() + " err " + errStr);
            throw new Exception("Status Code : " + conn.getResponseCode() + " Msg : " + errStr);
        } else {
            in = conn.getInputStream();
            streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String responseStr = readStringFrom(streamReader).toString();
            Log.d(TAG, "请求成功 " + responseStr);
            return responseStr;
        }
    }

    private HttpURLConnection getConnection(String url) throws IOException {
        final String host = new URL(url).getHost();
        HttpURLConnection conn = null;
        HTTPDNSResult result;
        /* 切换为新版标准api */
        if (async) {
            result = MyApp.getInstance().getService().getHttpDnsResultForHostAsync(host, type);
        } else {
            result = MyApp.getInstance().getService().getHttpDnsResultForHostSync(host, type);
        }
        Log.d(TAG, "httpdns 解析 " + host + " 结果为 " + result + " ttl is " + Util.getTtl(result));

        // 这里需要根据实际情况选择使用ipv6地址 还是 ipv4地址， 下面示例的代码优先使用了ipv6地址
        if (result.getIpv6s() != null && result.getIpv6s().length > 0 && HttpDnsNetworkDetector.getInstance().getNetType(context) != NetType.v4) {
            String newUrl = url.replace(host, "[" + result.getIpv6s()[0] + "]");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("Host", host);
            Log.d(TAG, "使用ipv6地址 " + newUrl);
        } else if (result.getIps() != null && result.getIps().length > 0 && HttpDnsNetworkDetector.getInstance().getNetType(context) != NetType.v6) {
            String newUrl = url.replace(host, result.getIps()[0]);
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("Host", host);
            Log.d(TAG, "使用ipv4地址 " + newUrl);
        }

        if (conn == null) {
            Log.d(TAG, "httpdns 未返回解析结果，走localdns");
            conn = (HttpURLConnection) new URL(url).openConnection();
        }
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(false);
        if (conn instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) conn;
            WebviewTlsSniSocketFactory sslSocketFactory = new WebviewTlsSniSocketFactory(
                (HttpsURLConnection)conn);

            // sni场景，创建SSLSocket
            httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
            // https场景，证书校验
            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    String host = httpsURLConnection.getRequestProperty("Host");
                    if (null == host) {
                        host = httpsURLConnection.getURL().getHost();
                    }
                    return HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session);
                }
            });
        }
        int code = conn.getResponseCode();// Network block
        if (needRedirect(code)) {
            //临时重定向和永久重定向location的大小写有区分
            String location = conn.getHeaderField("Location");
            if (location == null) {
                location = conn.getHeaderField("location");
            }
            if (!(location.startsWith("http://") || location
                    .startsWith("https://"))) {
                //某些时候会省略host，只返回后面的path，所以需要补全url
                URL originalUrl = new URL(url);
                location = originalUrl.getProtocol() + "://"
                        + originalUrl.getHost() + location;
            }
            return getConnection(location);
        }
        return conn;
    }

    private boolean needRedirect(int code) {
        return code >= 300 && code < 400;
    }

    static class WebviewTlsSniSocketFactory extends SSLSocketFactory {
        private final String TAG = WebviewTlsSniSocketFactory.class.getSimpleName();
        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        private HttpsURLConnection conn;

        public WebviewTlsSniSocketFactory(HttpsURLConnection conn) {
            this.conn = conn;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return null;
        }

        // TLS layer

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
            String peerHost = this.conn.getRequestProperty("Host");
            if (peerHost == null)
                peerHost = host;
            Log.i(TAG, "customized createSocket. host: " + peerHost);
            InetAddress address = plainSocket.getInetAddress();
            if (autoClose) {
                // we don't need the plainSocket
                plainSocket.close();
            }
            // create and connect SSL socket, but don't do hostname/certificate verification yet
            SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
            SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(address, port);

            // enable TLSv1.1/1.2 if available
            ssl.setEnabledProtocols(ssl.getSupportedProtocols());

            // set up SNI before the handshake
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.i(TAG, "Setting SNI hostname");
                sslSocketFactory.setHostname(ssl, peerHost);
            } else {
                Log.d(TAG, "No documented SNI support on Android <4.2, trying with reflection");
                try {
                    java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
                    setHostnameMethod.invoke(ssl, peerHost);
                } catch (Exception e) {
                    Log.w(TAG, "SNI not useable", e);
                }
            }

            // verify hostname and certificate
            SSLSession session = ssl.getSession();

            if (!hostnameVerifier.verify(peerHost, session))
                throw new SSLPeerUnverifiedException("Cannot verify hostname: " + peerHost);

            Log.i(TAG, "Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
                    " using " + session.getCipherSuite());

            return ssl;
        }
    }

    /**
     * stream to string
     */
    public static StringBuilder readStringFrom(BufferedReader streamReader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = streamReader.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }
}
