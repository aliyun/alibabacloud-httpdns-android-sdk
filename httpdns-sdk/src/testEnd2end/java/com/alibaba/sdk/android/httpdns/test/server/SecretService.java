package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class SecretService {
    HashMap<String, String> secrets = new HashMap<>();

    /**
     * 服务侧 校验 签名
     * @param secretService
     * @param recordedRequest
     * @return
     */
    public static boolean checkSign(SecretService secretService, RecordedRequest recordedRequest) {
        List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
        if (pathSegments.size() == 2 && (pathSegments.contains("sign_resolve") || pathSegments.contains("sign_d"))) {
            String account = pathSegments.get(0);
            String host = recordedRequest.getRequestUrl().queryParameter("host");
            String timestamp = recordedRequest.getRequestUrl().queryParameter("t");
            String secret = secretService.get(account);
            String sign = recordedRequest.getRequestUrl().queryParameter("s");
            long time = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            boolean timeValid = time > currentTime && time <= currentTime + 600;
            try {
                boolean signValid = sign.equals(CommonUtil.getMD5String(host + "-" + secret + "-" + timestamp));
                if (!timeValid || !signValid) {
                    TestLogger.log("check sign fail time : " + timeValid + " sign : " + signValid);
                }
                return timeValid && signValid;
            } catch (NoSuchAlgorithmException e) {
                TestLogger.log("check sign fail");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public String get(String account) {
        String secret = secrets.get(account);
        if (secret == null) {
            secret = RandomValue.randomStringWithFixedLength(16);
            secrets.put(account, secret);
        }
        return secret;
    }
}
