package com.alibaba.sdk.android.httpdns.impl;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignService {
	private static final String ALGORITHM = "HmacSHA256";
	public static final int EXPIRATION_TIME = 10 * 60;

	private static final String SDNS_PREFIX = "sdns-";
	private static final Set<String> includeParamKeys = new HashSet<>();

	private String mSecretKey;

	private long mOffset = 0L;

	public SignService(String secretKey) {
		mSecretKey = secretKey;
		includeParamKeys.addAll(Arrays.asList("id", "m", "dn", "cip", "q", "enc", "exp", "tags", "v"));
	}

	/**
	 * 设置密钥
	 */
	public void setSecretKey(String secretKey) {
		mSecretKey = secretKey;
	}

	/**
	 * 使用 HMAC-SHA256 生成十六进制格式的签名
	 */
	public String sign(Map<String, String> param) {
		if (TextUtils.isEmpty(mSecretKey)) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("secretKey为空.");
			}
			return "";
		}

		String signStr;
		try {
			signStr = generateV2SignContent(param);
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.d("signStr:" + signStr);
			}
			signStr = hmacSha256(signStr);
		} catch (Exception e) {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.e("sign fail.", e);
			}
			signStr = "";
		}
		return signStr;
	}

	private static String generateV2SignContent(Map<String, String> map) {
		Map<String, String> sortedMap = new TreeMap<>();
		for(Map.Entry<String, String> entry : map.entrySet()) {
			String paramKey = entry.getKey();
			String paramValue = entry.getValue();
			if (includeParamKeys.contains(paramKey) || paramKey.startsWith(SDNS_PREFIX)) {
				if(!TextUtils.isEmpty(paramValue)) {
					sortedMap.put(paramKey, paramValue);
				}
			}
		}
		StringBuilder signContent = new StringBuilder();
		for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
			if (signContent.length() > 0) {
				signContent.append("&");
			}
			signContent.append(entry.getKey()).append("=").append(entry.getValue());
		}
		return signContent.toString();
	}

	private String hmacSha256(String content)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance(ALGORITHM);
		mac.init(new SecretKeySpec(CommonUtil.decodeHex(mSecretKey), ALGORITHM));
		byte[] signedBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
		return CommonUtil.encodeHexString(signedBytes);
	}

	public void setCurrentTimestamp(long serverTime) {
		mOffset = serverTime - System.currentTimeMillis() / 1000;
	}

	public String getExpireTime(){
		return Long.toString(System.currentTimeMillis() / 1000 + EXPIRATION_TIME + mOffset);
	}

	public Boolean isSignMode(){
		return !TextUtils.isEmpty(mSecretKey);
	}
}
