package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.request.ResponseParser;

public class BatchResolveHostResponseParser implements
	ResponseParser<BatchResolveHostResponse> {
	@Override
	public BatchResolveHostResponse parse(String response) throws Throwable {
		return BatchResolveHostResponse.fromResponse(response);
	}
}
