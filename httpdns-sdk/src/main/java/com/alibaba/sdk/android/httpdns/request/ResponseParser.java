package com.alibaba.sdk.android.httpdns.request;

/**
 * http响应解析
 */
public interface ResponseParser<T> {
    T parse(String response) throws Throwable;
}
