package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
@RunWith(RobolectricTestRunner.class)
public class SignServiceTest {

    @Test
    public void noSign() {
        SignService service = new SignService(null);
        HashMap<String, String> params = service.getSigns(RandomValue.randomHost());
        MatcherAssert.assertThat("没有密钥不签名", params == null);
    }

    @Test
    public void testSign() throws NoSuchAlgorithmException {
        String secret = RandomValue.randomStringWithFixedLength(10);
        SignService service = new SignService(secret);
        String host = RandomValue.randomHost();
        HashMap<String, String> params = service.getSigns(host);
        String t = params.get("t");
        String s = params.get("s");
        MatcherAssert.assertThat("验证签名算法逻辑", s.equals(CommonUtil.getMD5String(host + "-" + secret + "-" + t)));

        String secret1 = RandomValue.randomStringWithFixedLength(10);
        service.setSecret(secret1);
        params = service.getSigns(host);
        t = params.get("t");
        s = params.get("s");
        MatcherAssert.assertThat("验证密钥更新的情况", s.equals(CommonUtil.getMD5String(host + "-" + secret1 + "-" + t)));
    }

    @Test
    public void testTimestamp() throws NoSuchAlgorithmException {
        String secret = RandomValue.randomStringWithFixedLength(10);
        SignService service = new SignService(secret);

        service.setCurrentTimestamp(System.currentTimeMillis() / 1000 + 10);

        String host = RandomValue.randomHost();
        HashMap<String, String> params = service.getSigns(host);
        String t = params.get("t");
        String s = params.get("s");
        MatcherAssert.assertThat("验证签名算法逻辑", s.equals(CommonUtil.getMD5String(host + "-" + secret + "-" + t)));

        long requestTime = Long.parseLong(t);
        long abs = Math.abs(requestTime - 10 - System.currentTimeMillis() / 1000 - 600);
        MatcherAssert.assertThat("请求有效期是10分钟，设置的校正偏差是10s. 差距： " + abs, abs < 2);
    }


}
