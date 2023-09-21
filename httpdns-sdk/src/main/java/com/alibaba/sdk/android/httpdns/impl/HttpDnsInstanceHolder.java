package com.alibaba.sdk.android.httpdns.impl;

import java.util.HashMap;

import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import android.content.Context;

/**
 * HttpDnsService 实例持有者
 */
public class HttpDnsInstanceHolder {

	private final HttpDnsCreator mHttpDnsCreator;
	private final HashMap<String, HttpDnsService> mInstances;
	private final ErrorImpl mError = new ErrorImpl();

	public HttpDnsInstanceHolder(HttpDnsCreator creator) {
		this.mHttpDnsCreator = creator;
		mInstances = new HashMap<>();
	}

	public HttpDnsService get(Context context, String account, String secretKey) {
		if (context == null) {
			HttpDnsLog.e("init httpdns with null context!!");
			return mError;
		}
		if (account == null || account.equals("")) {
			HttpDnsLog.e("init httpdns with emtpy account!!");
			return mError;
		}
		HttpDnsService service = mInstances.get(account);
		if (service == null) {

			service = mHttpDnsCreator.create(context, account, secretKey);
			mInstances.put(account, service);
		} else {
			if (service instanceof HttpDnsServiceImpl) {
				((HttpDnsServiceImpl)service).setSecret(secretKey);
			}
		}
		return service;
	}
}
