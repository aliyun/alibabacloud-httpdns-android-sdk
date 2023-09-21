package com.alibaba.sdk.android.httpdns.interpret;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.impl.SignService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;

public class InterpretHostHelper {

	public static HttpRequestConfig getIpv4Config(HttpDnsConfig config, String host) {
		return getConfig(config, host, RequestIpType.v4, null, null, null);
	}

	public static HttpRequestConfig getIpv6Config(HttpDnsConfig config, String host) {
		return getConfig(config, host, RequestIpType.v6, null, null, null);
	}

	public static HttpRequestConfig getConfig(HttpDnsConfig config, String host,
											  RequestIpType type,
											  Map<String, String> extras, String cacheKey,
											  Map<String, String> globalParams) {
		HashMap<String, String> extraArgs = null;
		if (cacheKey != null) {
			extraArgs = new HashMap<>();
			if (globalParams != null) {
				extraArgs.putAll(globalParams);
			}
			if (extras != null) {
				extraArgs.putAll(extras);
			}
		}
		String path = getPath(config, host, type, extraArgs, null);
		if (config.getNetworkDetector() != null && config.getNetworkDetector().getNetType(
			config.getContext()) == NetType.v6) {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIpForV6(),
				config.getCurrentServer().getPortForV6(), path, config.getTimeout(),
				RequestIpType.v6);
		} else {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort(), path,
				config.getTimeout(), RequestIpType.v4);
		}
	}

	public static HttpRequestConfig getConfig(HttpDnsConfig config, String host,
											  RequestIpType type,
											  Map<String, String> extras, String cacheKey,
											  Map<String, String> globalParams,
											  SignService signService) {
		HashMap<String, String> extraArgs = null;
		if (cacheKey != null) {
			extraArgs = new HashMap<>();
			if (globalParams != null) {
				extraArgs.putAll(globalParams);
			}
			if (extras != null) {
				extraArgs.putAll(extras);
			}
		}
		HashMap<String, String> signParams = signService.getSigns(host);
		String path = getPath(config, host, type, extraArgs, signParams);
		if (config.getNetworkDetector() != null && config.getNetworkDetector().getNetType(
			config.getContext()) == NetType.v6) {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIpForV6(),
				config.getCurrentServer().getPortForV6(), path, config.getTimeout(),
				RequestIpType.v6);
		} else {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort(), path,
				config.getTimeout(), RequestIpType.v4);
		}
	}

	public static String getPath(HttpDnsConfig config, String host, RequestIpType type,
								 Map<String, String> extras, HashMap<String, String> params) {
		String query = getQuery(type);
		String extra = null;
		try {
			extra = getExtra(extras);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String urlParams = toUrlParams(params);
		String service = "d";
		if (params != null && params.containsKey("s")) {
			service = "sign_d";
		}
		return "/" + config.getAccountId() + "/" + service + "?host=" + host + "&sdk=android_"
			+ BuildConfig.VERSION_NAME + query + getSid() + extra + urlParams;
	}

	private static String toUrlParams(HashMap<String, String> params) {
		if (params == null || params.size() == 0) {
			return "";
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			stringBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
		}
		return stringBuilder.toString();
	}

	private static String getQuery(RequestIpType type) {
		String query = "";
		switch (type) {
			case v6:
				query = "&query=6";
				break;
			case both:
				query = "&query=4,6";
				break;
			default:
				break;
		}
		return query;
	}

	private static String getExtra(Map<String, String> extras) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		boolean isKey = true;
		boolean isValue = true;
		if (extras != null) {
			for (Map.Entry<String, String> entry : extras.entrySet()) {
				sb.append("&sdns-");
				sb.append(entry.getKey());
				sb.append("=");
				sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				if (!checkKey(entry.getKey())) {
					isKey = false;
					HttpDnsLog.e("设置自定义参数失败，自定义key不合法：" + entry.getKey());
					break;
				}
				if (!checkValue(entry.getValue())) {
					isValue = false;
					HttpDnsLog.e("设置自定义参数失败，自定义value不合法：" + entry.getValue());
					break;
				}
			}
		} else {
			return "";
		}
		if (isKey && isValue) {
			String extra = sb.toString();
			if (extra.getBytes("UTF-8").length <= 1000) {
				return extra;
			} else {
				HttpDnsLog.e("设置自定义参数失败，自定义参数过长");
				return "";
			}
		} else {
			return "";
		}
	}

	private static boolean checkKey(String s) {
		return s.matches("[a-zA-Z0-9\\-_]+");
	}

	private static boolean checkValue(String s) {
		return s.matches("[a-zA-Z0-9\\-_=]+");
	}

	public static HttpRequestConfig getConfig(HttpDnsConfig config, ArrayList<String> hostList,
											  RequestIpType type, SignService signService) {
		String query = getQuery(type);
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < hostList.size(); i++) {
			if (i != 0) {
				stringBuilder.append(",");
			}
			stringBuilder.append(hostList.get(i));
		}
		String host = stringBuilder.toString();
		HashMap<String, String> signs = signService.getSigns(host);
		String service = "resolve";
		if (signs != null && signs.containsKey("s")) {
			service = "sign_resolve";
		}
		String path = "/" + config.getAccountId() + "/" + service + "?host=" + host
			+ "&sdk=android_" + BuildConfig.VERSION_NAME + query + getSid() + toUrlParams(signs);
		if (config.getNetworkDetector() != null && config.getNetworkDetector().getNetType(
			config.getContext()) == NetType.v6) {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIpForV6(),
				config.getCurrentServer().getPortForV6(), path, config.getTimeout(),
				RequestIpType.v6);
		} else {
			return new HttpRequestConfig(config.getSchema(),
				config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort(), path,
				config.getTimeout(), RequestIpType.v4);
		}
	}

	public static String getSid() {
		String sessionId = SessionTrackMgr.getInstance().getSessionId();
		if (sessionId == null) {
			return "";
		} else {
			return "&sid=" + sessionId;
		}
	}
}
