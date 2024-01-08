package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 网络请求的配置
 */
public class HttpRequestConfig {
    public static final String HTTP_SCHEMA = "http://";
    public static final String HTTPS_SCHEMA = "https://";
    public static final String HTTPS_CERTIFICATE_HOSTNAME = "203.107.1.1";
    /**
     * 请求协议
     */
    private String mSchema = HTTP_SCHEMA;
    /**
     * 服务器ip
     */
    private String mIp;
    /**
     * 服务器端口
     */
    private int mPort;
    /**
     * 请求路径，包含query参数
     */
    private String mPath;
    /**
     * 请求超时时间
     */
    private int mTimeout = Constants.DEFAULT_TIMEOUT;

    /**
     * 服务器IP类型 只会是 v4 或者 v6
     */
    private RequestIpType mIpType = RequestIpType.v4;

    public HttpRequestConfig(String ip, int port, String path) {
        this.mIp = ip;
        this.mPort = port;
        this.mPath = path;
    }

    public HttpRequestConfig(String schema, String ip, int port, String path, int timeout, RequestIpType ipType) {
        this.mSchema = schema;
        this.mIp = ip;
        this.mPort = port;
        this.mPath = path;
        this.mTimeout = timeout;
        this.mIpType = ipType;
    }

    public void setSchema(String schema) {
        this.mSchema = schema;
    }

    public void setIp(String ip) {
        this.mIp = ip;
    }

    public void setPort(int port) {
        this.mPort = port;
    }

    public String getSchema() {
        return this.mSchema;
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }

    public void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }

    public int getTimeout() {
        return mTimeout;
    }

    public RequestIpType getIpType() {
        return mIpType;
    }

    public String url() {
        if (mIpType == RequestIpType.v6) {
            return mSchema + "[" + mIp + "]" + ":" + mPort + mPath;
        } else {
            return mSchema + mIp + ":" + mPort + mPath;
        }
    }
}
