package com.alibaba.sdk.android.httpdns.serverip;

import java.util.ArrayList;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.ResponseParser;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import android.text.TextUtils;

import static com.alibaba.sdk.android.httpdns.resolve.ResolveHostHelper.getSid;

public class UpdateRegionServerTask {

	public static void updateRegionServer(HttpDnsConfig config, String region,
										  RequestCallback<UpdateRegionServerResponse> callback) {
		String path = "/" + config.getAccountId() + "/ss?"
			+ "platform=android&sdk_version=" + BuildConfig.VERSION_NAME
			+ ((TextUtils.isEmpty(region) ? "" : ("&region=" + region))
			+ getSid());

		RegionServer[] servers;

		RequestIpType ipType;
		HttpDnsSettings.NetworkDetector networkDetector = config.getNetworkDetector();
		if (networkDetector != null && networkDetector.getNetType(config.getContext())
			== NetType.v6) {
			servers = getAllServers(
				config.getCurrentServer().getIpv6ServerIps(),
				config.getCurrentServer().getIpv6Ports(),
				config.getInitServer().getIpv6ServerIps(), config.getInitServer().getIpv6Ports(),
				config.getDefaultUpdateServer().getIpv6ServerIps(),
				config.getDefaultUpdateServer().getIpv6Ports());
			ipType = RequestIpType.v6;
		} else {
			servers = getAllServers(
				config.getCurrentServer().getServerIps(), config.getCurrentServer().getPorts(),
				config.getInitServer().getServerIps(), config.getInitServer().getPorts(),
				config.getDefaultUpdateServer().getServerIps(),
				config.getDefaultUpdateServer().getPorts());
			ipType = RequestIpType.v4;
		}

		HttpRequestConfig requestConfig = new HttpRequestConfig(config.getSchema(),
			servers[0].getServerIp(), servers[0].getPort(config.getSchema()), path,
			config.getTimeout(), ipType);
		HttpRequest<UpdateRegionServerResponse> httpRequest = new HttpRequest<>(requestConfig,
			new ResponseParser<UpdateRegionServerResponse>() {
				@Override
				public UpdateRegionServerResponse parse(String response) throws Throwable {
					return UpdateRegionServerResponse.fromResponse(response);
				}
			});
		httpRequest = new HttpRequestWatcher<>(httpRequest, new HttpRequestFailWatcher(
			ReportManager.getReportManagerByAccount(config.getAccountId())));
		// 增加切换ip，回到初始Ip的逻辑
		httpRequest = new HttpRequestWatcher<>(httpRequest, new ShiftRegionServerWatcher(servers));
		// 重试，当前服务Ip和初始服务ip个数
		httpRequest = new RetryHttpRequest<>(httpRequest, servers.length - 1);

		try {
			config.getWorker().execute(new HttpRequestTask<>(httpRequest, callback));
		} catch (Throwable e) {
			callback.onFail(e);
		}
	}

	private static RegionServer[] getAllServers(String[] currentServerIps, int[] ports,
												String[] initServerIps, int[] initServerPorts,
												String[] defaultServerIps, int[] defaultServerPorts) {
		ArrayList<String> serverIps = new ArrayList<>();
		ArrayList<Integer> serverPorts = new ArrayList<>();
		if (currentServerIps != null) {
			for (int i = 0; i < currentServerIps.length; i++) {
				serverIps.add(currentServerIps[i]);
				serverPorts.add(ports != null && ports.length > i ? ports[i] : Constants.NO_PORT);
			}
		}
		if (initServerIps != null) {
			for (int i = 0; i < initServerIps.length; i++) {
				serverIps.add(initServerIps[i]);
				serverPorts.add(
					initServerPorts != null && initServerPorts.length > i ? initServerPorts[i]
						: Constants.NO_PORT);
			}
		}
		if (defaultServerIps != null) {
			for (int i = 0; i < defaultServerIps.length; i++) {
				serverIps.add(defaultServerIps[i]);
				serverPorts.add(defaultServerPorts != null && defaultServerPorts.length > i
					? defaultServerPorts[i] : Constants.NO_PORT);
			}
		}

		RegionServer[] servers = new RegionServer[serverIps.size()];
		for (int i = 0; i < serverIps.size(); i++) {
			servers[i] = new RegionServer(serverIps.get(i), serverPorts.get(i));
		}
		return servers;
	}

	private static String[] getAllIpv6Servers(String[] ips1, String[] ips2) {
		ArrayList<String> ips = new ArrayList<>();
		if (ips1 != null) {
            for (String s : ips1) {
                ips.add(s);
            }
		}
		if (ips2 != null) {
            for (String s : ips2) {
                ips.add(s);
            }
		}
		return ips.toArray(new String[0]);
	}
}
