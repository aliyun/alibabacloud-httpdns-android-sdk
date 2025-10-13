package com.alibaba.sdk.android.httpdns.resolve;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.impl.AESEncryptService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.ResponseParser;

import org.json.JSONObject;

public class ResolveHostResponseParser implements
	ResponseParser<ResolveHostResponse> {

	private final AESEncryptService mAESEncryptService;

	public ResolveHostResponseParser(AESEncryptService aesEncryptService) {
		mAESEncryptService = aesEncryptService;
	}

	@Override
	public ResolveHostResponse parse(String serverIp, String response) throws Throwable {
		String data = "";
		JSONObject jsonResponse = new JSONObject(response);
		if (jsonResponse.has("code")) {
			String code = jsonResponse.getString("code");
			if (TextUtils.equals(code, "success")) {
				if (jsonResponse.has("data")) {
					data = jsonResponse.getString("data");
					if (!TextUtils.isEmpty(data)) {
						//解密
						AESEncryptService.EncryptionMode mode = AESEncryptService.EncryptionMode.PLAIN;
						if (jsonResponse.has("mode")) {
							String serverEncryptMode = jsonResponse.getString("mode");
							if (TextUtils.equals(serverEncryptMode, AESEncryptService.EncryptionMode.AES_GCM.getMode())) {
								mode = AESEncryptService.EncryptionMode.AES_GCM;
							} else if (TextUtils.equals(serverEncryptMode, AESEncryptService.EncryptionMode.AES_CBC.getMode())) {
								mode = AESEncryptService.EncryptionMode.AES_CBC;
							}
						}
						data = mAESEncryptService.decrypt(data, mode);
						if (TextUtils.isEmpty(data)) {
							if (HttpDnsLog.isPrint()) {
								HttpDnsLog.e("response data decrypt fail");
							}
						}
					} else {
						if (HttpDnsLog.isPrint()) {
							HttpDnsLog.e("response data is empty");
						}
					}
				}
			} else {
				if (HttpDnsLog.isPrint()) {
					HttpDnsLog.e("解析失败,原因为" + code);
				}
				throw new Exception(code);
			}
		}else {
			if (HttpDnsLog.isPrint()) {
				HttpDnsLog.e("response don't have code");
			}
		}
		if (HttpDnsLog.isPrint()) {
			HttpDnsLog.d("request success " + data);
		}
		return ResolveHostResponse.fromResponse(serverIp, data);
	}
}
