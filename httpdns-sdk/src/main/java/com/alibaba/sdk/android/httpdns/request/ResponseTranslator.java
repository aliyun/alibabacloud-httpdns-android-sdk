package com.alibaba.sdk.android.httpdns.request;

/**
 * http响应解析
 */
public interface ResponseTranslator<T> {
    T translate(String response) throws Throwable;
}
