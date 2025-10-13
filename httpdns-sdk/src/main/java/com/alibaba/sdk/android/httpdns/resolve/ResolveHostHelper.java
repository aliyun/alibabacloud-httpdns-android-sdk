package com.alibaba.sdk.android.httpdns.resolve;

import android.os.Build;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.AESEncryptService;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.impl.SignService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;

import org.json.JSONObject;

public class ResolveHostHelper {
	public static HttpRequestConfig getConfig(HttpDnsConfig config, String host,
											  RequestIpType type,
											  Map<String, String> extras, String cacheKey,
											  Map<String, String> globalParams,
											  SignService signService,
											  AESEncryptService encryptService) {
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

		String path = getPath(config, host, type, extraArgs, signService, encryptService);
		HttpRequestConfig requestConfig = getHttpRequestConfig(config, path, signService.isSignMode());
		requestConfig.setUA(config.getUA());
		requestConfig.setAESEncryptService(encryptService);
		return requestConfig;
	}

	public static String getPath(HttpDnsConfig config, String host, RequestIpType type,
								 Map<String, String> extras,
								 SignService signService,
								 AESEncryptService encryptService) {
		//参数加密
		String enc = "";
		String query = getQuery(type);
		String version = "1.0";
		String tags = config.getBizTags();
		AESEncryptService.EncryptionMode mode = AESEncryptService.EncryptionMode.PLAIN;
		if (encryptService.isEncryptionMode()) {
			String encryptJson = buildEncryptionStr(host, query, extras, tags);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("encryptJson:" + encryptJson);
			}
			mode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? AESEncryptService.EncryptionMode.AES_GCM : AESEncryptService.EncryptionMode.AES_CBC;
			enc = encryptService.encrypt(encryptJson, mode);
		}

		String expireTime = signService.getExpireTime();

		String queryStr = buildQueryStr(config.getAccountId(), mode.getMode(), host,
				query, extras, enc, expireTime, version, tags);
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("query parameter:" + queryStr);
		}

		//加签
		if (signService.isSignMode()) {
			Map<String, String> signParamMap = new HashMap<>();
			if (encryptService.isEncryptionMode()) {
				signParamMap.put("enc", enc);
				signParamMap.put("exp", expireTime);
				signParamMap.put("id", config.getAccountId());
				signParamMap.put("m", mode.getMode());
				signParamMap.put("v", version);
			}else {
				signParamMap.put("dn", host);
				signParamMap.put("exp", expireTime);
				signParamMap.put("id", config.getAccountId());
				signParamMap.put("m", mode.getMode());
				if (!TextUtils.isEmpty(query)) {
					signParamMap.put("q", query);
				}
				if (extras != null) {
					for (Map.Entry<String, String> entry : extras.entrySet()) {
						signParamMap.put("sdns-" + entry.getKey(), entry.getValue());
					}
				}
				if (!TextUtils.isEmpty(tags)) {
					signParamMap.put("tags", tags);
				}
				signParamMap.put("v", version);
			}

			String sign = signService.sign(signParamMap);
			if (TextUtils.isEmpty(sign)) {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("param sign fail");
				}
			}else {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.d("sign:" + sign);
				}
				queryStr += "&s=" + sign;
			}
		}
		String path = "/v2/d?" + queryStr + "&sdk=android_" + BuildConfig.VERSION_NAME + getSid();
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("path：" + path);
		}
		return path;
	}

	private static String buildQueryStr(String accountId, String mode, String host,
										String query, Map<String, String> extras, String enc,
										String expireTime, String version, String tags) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("id=").append(accountId);
		stringBuilder.append("&m=").append(mode);
		if (TextUtils.isEmpty(enc)) {
			stringBuilder.append("&dn=").append(host);
			if (!TextUtils.isEmpty(query)) {
				stringBuilder.append("&q=").append(query);
			}

			String extra = getExtra(extras);
			if (!TextUtils.isEmpty(extra)) {
				stringBuilder.append(extra);
			}
			if (!TextUtils.isEmpty(tags)) {
				stringBuilder.append("&tags=").append(tags);
			}
		}else {
			stringBuilder.append("&enc=").append(enc);
		}
		stringBuilder.append("&v=").append(version);
		stringBuilder.append("&exp=").append(expireTime);
		return stringBuilder.toString();
	}

	private static String buildEncryptionStr(String host, String query, Map<String, String> extras, String tags) {
		JSONObject json = new JSONObject();
		try {
			json.put("dn", host);
			if (!TextUtils.isEmpty(query)) {
				json.put("q", query);
			}
			if (!TextUtils.isEmpty(getExtra(extras))) {
				for (Map.Entry<String, String> entry : extras.entrySet()) {
					if (!checkKey(entry.getKey()) || !checkValue(entry.getValue())) {
						continue;
					}
					json.put("sdns-" + entry.getKey(), entry.getValue());
				}
			}
			if (!TextUtils.isEmpty(tags)) {
				json.put("tags", tags);
			}
		} catch (Exception e) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.e("encrypt param transfer to json fail.", e);
			}

		}
		return json.toString();
	}

	private static String getQuery(RequestIpType type) {
		String query = "";
		switch (type) {
			case v6:
				query = "6";
				break;
			case both:
				query = "4,6";
				break;
			default:
				break;
		}
		return query;
	}

	private static String getExtra(Map<String, String> extras) {
		StringBuilder sb = new StringBuilder();
		boolean isKey = true;
		boolean isValue = true;
		if (extras != null) {
			for (Map.Entry<String, String> entry : extras.entrySet()) {
				sb.append("&sdns-");
				sb.append(entry.getKey());
				sb.append("=");
				sb.append(entry.getValue());
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
			if (extra.getBytes(StandardCharsets.UTF_8).length <= 1000) {
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
											  RequestIpType type, SignService signService,
											  AESEncryptService encryptService) {
		//拼接host
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < hostList.size(); i++) {
			if (i != 0) {
				stringBuilder.append(",");
			}
			stringBuilder.append(hostList.get(i));
		}
		String host = stringBuilder.toString();
		String path = getPath(config, host, type, null, signService, encryptService);
		HttpRequestConfig requestConfig = getHttpRequestConfig(config, path, signService.isSignMode());
		requestConfig.setUA(config.getUA());
		requestConfig.setAESEncryptService(encryptService);
		return requestConfig;
	}

	private static HttpRequestConfig getHttpRequestConfig(HttpDnsConfig config, String path, Boolean isSignMode) {
		if (config.getNetworkDetector() != null && config.getNetworkDetector().getNetType(
				config.getContext()) == NetType.v6) {
			return new HttpRequestConfig(config.getSchema(),
					config.getCurrentServer().getServerIpForV6(),
					config.getCurrentServer().getPortForV6(), path, config.getTimeout(),
					RequestIpType.v6, isSignMode);
		} else {
			return new HttpRequestConfig(config.getSchema(),
					config.getCurrentServer().getServerIp(), config.getCurrentServer().getPort(), path,
					config.getTimeout(), RequestIpType.v4, isSignMode);

		}
	}

	public static String getTags(HttpDnsConfig config) {
		if (TextUtils.isEmpty(config.getBizTags())) {
			return "";
		}

		return "&tags=" + config.getBizTags();
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
