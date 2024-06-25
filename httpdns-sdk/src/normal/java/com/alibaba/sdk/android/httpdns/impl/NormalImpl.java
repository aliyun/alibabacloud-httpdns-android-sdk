package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.report.ReportManager;

public class NormalImpl extends HttpDnsServiceImpl {
	public NormalImpl(Context context, String accountId, String secret) {
		super(context, accountId, secret);
	}

	@Override
	protected void favorInit(Context context, String accountId) {
		super.favorInit(context, accountId);
		ReportManager.init(context);
		ReportManager reportManager = ReportManager.getReportManagerByAccount(accountId);
		reportManager.setAccountId(accountId);
	}
}
