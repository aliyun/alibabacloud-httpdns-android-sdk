package com.alibaba.sdk.android.httpdns.impl;

import android.app.Application;
import android.content.Context;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.beacon.BeaconControl;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.sender.AlicloudSender;
import com.alibaba.sdk.android.sender.SdkInfo;

import java.util.HashMap;

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
		reportSdkStart(context, accountId);
		initBeacon(context, accountId, mHttpDnsConfig);
	}

	protected void initBeacon(Context context, String accountId, HttpDnsConfig config) {
		BeaconControl.initBeacon(context, accountId, config);
	}

	protected void reportSdkStart(Context context, String accountId) {
		if (HttpDnsSettings.isDailyReport()) {
			try {
				HashMap<String, String> ext = new HashMap<>();
				ext.put("accountId", accountId);
				SdkInfo sdkInfo = new SdkInfo();
				sdkInfo.setSdkId("httpdns");
				sdkInfo.setSdkVersion(BuildConfig.VERSION_NAME);
				sdkInfo.setExt(ext);
				if (context.getApplicationContext() instanceof Application) {
					AlicloudSender.asyncSend((Application)context.getApplicationContext(),
                        sdkInfo);
				} else {
					AlicloudSender.asyncSend(context.getApplicationContext(), sdkInfo);
				}
			} catch (Throwable ignore) {
				ignore.printStackTrace();
			}
		}
	}
}
