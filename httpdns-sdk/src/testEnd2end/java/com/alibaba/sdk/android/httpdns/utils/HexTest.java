package com.alibaba.sdk.android.httpdns.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by renwei
 * on 2025/04/18
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class HexTest {

    @Test
    public void testHex() {
        String hex = "82c0af0d0cb2d69c4f87aa34";
        try {
            byte[] bytes = CommonUtil.decodeHex(hex);
            assertNotNull(bytes);
            String result = CommonUtil.encodeHexString(bytes);
            assertEquals(hex, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBase64() {
        String base64 = "YWxpYW5vbmU=";
        try {
            byte[] bytes = CommonUtil.decodeBase64(base64);
            assertNotNull(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
