package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.request.ResponseTranslator;

/**
 * 解析 域名解析请求结果
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class InterpretHostResponseTranslator implements ResponseTranslator<InterpretHostResponse> {
    @Override
    public InterpretHostResponse translate(String response) throws Throwable {
        return InterpretHostResponse.fromResponse(response);
    }
}
