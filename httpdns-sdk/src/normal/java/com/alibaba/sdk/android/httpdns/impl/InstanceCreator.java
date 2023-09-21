package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

public class InstanceCreator implements HttpDnsCreator {
	@Override
	public HttpDnsService create(Context context, String accountId, String secretKey) {
		return new NormalImpl(context, accountId, secretKey);
	}
}
