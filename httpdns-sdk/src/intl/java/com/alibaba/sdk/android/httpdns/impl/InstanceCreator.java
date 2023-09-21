package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

import android.content.Context;

public class InstanceCreator implements HttpDnsCreator {
	@Override
	public HttpDnsService create(Context context, String accountId, String secretKey) {
		return new IntlImpl(context, accountId, secretKey);
	}
}
