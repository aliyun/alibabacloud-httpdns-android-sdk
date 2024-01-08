package com.alibaba.sdk.android.httpdns.impl;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

public class SignService {
	public static final int EXPIRATION_TIME = 10 * 60;
	private String mSecret;
	private long mOffset = 0L;

	public SignService(String secret) {
		this.mSecret = secret;
	}

	public HashMap<String, String> getSigns(String host) {
		if (mSecret == null) {
			return null;
		}
		String t = Long.toString(System.currentTimeMillis() / 1000 + EXPIRATION_TIME + mOffset);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(host);
		stringBuilder.append("-");
		stringBuilder.append(mSecret);
		stringBuilder.append("-");
		stringBuilder.append(t);
		String s;
		try {
			s = CommonUtil.getMD5String(stringBuilder.toString());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		HashMap<String, String> params = new HashMap<>();
		params.put("t", t);
		params.put("s", s);
		return params;
	}

	public void setSecret(String secret) {
		this.mSecret = secret;
	}

	public void setCurrentTimestamp(long serverTime) {
		mOffset = serverTime - System.currentTimeMillis() / 1000;
	}
}
