package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.request.ResponseTranslator;


public class ResolveHostResponseTranslator implements ResponseTranslator<ResolveHostResponse> {
	@Override
	public ResolveHostResponse translate(String response) throws Throwable {
		return ResolveHostResponse.fromResponse(response);
	}
}
