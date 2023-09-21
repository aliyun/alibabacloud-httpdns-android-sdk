package com.alibaba.sdk.android.httpdns.beacon;

import android.content.Context;

import com.alibaba.sdk.android.beacon.Beacon;
import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.report.ReportManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconControl {

	public static void initBeacon(Context context, final String accountId,
								  final HttpDnsConfig httpDnsConfig) {
		if (context == null || accountId == null || accountId.isEmpty() || httpDnsConfig == null) {
			HttpDnsLog.w("params is empty");
			return;
		}
		Map<String, String> extras = new HashMap<String, String>();
		extras.put("sdkId", "httpdns");
		extras.put("sdkVer", BuildConfig.VERSION_NAME);
		extras.put("accountId", accountId);
		Beacon beacon = new Beacon.Builder().appKey("24657847").appSecret("f30fc0937f2b1e9e50a1b7134f1ddb10").startPoll(false).extras(extras)
			.build();
		beacon.addUpdateListener(new Beacon.OnUpdateListener() {
			@Override
			public void onUpdate(List<Beacon.Config> list) {
				if (list != null && list.size() > 0) {
					for (Beacon.Config config : list) {
						if (config.key.equals("___httpdns_service___")) {
							try {
								JSONObject jsonObject = new JSONObject(config.value);

								String status = jsonObject.optString("status", "normal");
								if (status.equals("disabled")) {
									httpDnsConfig.remoteDisable(true);
									HttpDnsLog.w("beacon disabled");
								} else {
									httpDnsConfig.remoteDisable(false);
									HttpDnsLog.d("beacon normal");
								}

								String report = jsonObject.optString("ut", "normal");
								if (report.equals("disabled")) {
									ReportManager.enableReport(accountId, false);
								} else {
									ReportManager.enableReport(accountId, true);
								}

								String probe = jsonObject.optString("ip-ranking", "normal");
								if (probe.equals("disabled")) {
									httpDnsConfig.probeDisable(true);
									HttpDnsLog.w("beacon probe disabled");
								} else {
									httpDnsConfig.probeDisable(false);
									HttpDnsLog.d("beacon probe normal");
								}

							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
		beacon.addServiceErrListener(new Beacon.OnServiceErrListener() {
			@Override
			public void onErr(Beacon.Error error) {
				HttpDnsLog.w("beacon err " + error.errCode + " " + error.errMsg);
			}
		});
		beacon.start(context);
	}
}
