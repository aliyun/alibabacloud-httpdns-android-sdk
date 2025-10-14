package com.alibaba.sdk.android.httpdns.observable;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWrapper;
import com.alibaba.sdk.android.httpdns.request.ResponseParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class ObservableHttpRequest<T> extends HttpRequestWrapper<T> {
    private String url;
    private ResponseParser<T> parser;
    private Map<String, String> mHeaders;
    private String mBody;

    public ObservableHttpRequest(String url, ResponseParser<T> parser, Map<String, String> headers, String body) {
        super(null);
        this.url = url;
        this.parser = parser;
        mHeaders = headers;
        mBody = body;
    }

    @Override
    public T request() throws Throwable {
        HttpURLConnection conn = null;
        InputStream in = null;
        BufferedReader streamReader = null;

        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("request url " + url);
        }

        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            if (mHeaders != null) {
                for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (!TextUtils.isEmpty(mBody)) {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                OutputStream outputStream = conn.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                bw.write(mBody);
                bw.close();
            }

            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = conn.getInputStream();
                streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String responseStr = readStringFrom(streamReader).toString();
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("request success " + responseStr);
                }

                return parser.parse(null, responseStr);
            }
        } catch (Throwable e) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d(e.getMessage());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (streamReader != null) {
                    streamReader.close();
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }
}
