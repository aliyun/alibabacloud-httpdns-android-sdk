package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.request.ResponseParser;

/**
 * 解析 域名解析请求结果
 */
public class ResolveInterpretHostResponseParser
	implements ResponseParser<ResolveHostResponse> {
	@Override
	public ResolveHostResponse parse(String response) throws Throwable {
		return ResolveHostResponse.fromResponse(response);
	}
}
