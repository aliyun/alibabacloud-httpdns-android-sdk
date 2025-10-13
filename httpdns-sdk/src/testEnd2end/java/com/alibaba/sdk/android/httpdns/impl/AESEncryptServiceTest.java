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
 * @author renwei
 * @date 2025/4/2
 */
@RunWith(RobolectricTestRunner.class)
class AESEncryptServiceTest {

    private AESEncryptService service;
    private static final String TEST_KEY = "0123456789ABCDEF0123456789ABCDEF"; // 32字节密钥
    private static final String TEST_DATA = "Hello World!";

    @Before
    public void setUp() {
        service = new AESEncryptService(TEST_KEY);
    }

    @Test
    public void testGCMEncryptionDecryption() {
        String encrypted = service.encrypt(TEST_DATA, EncryptionMode.AES_GCM);
        String decrypted = service.decrypt(encrypted, EncryptionMode.AES_GCM);
        assertEquals(TEST_DATA, decrypted);
    }

    @Test
    public void testCBCEncryptionDecryption() {
        String encrypted = service.encrypt(TEST_DATA, EncryptionMode.AES_CBC);
        String decrypted = service.decrypt(encrypted, EncryptionMode.AES_CBC);
        assertEquals(TEST_DATA, decrypted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyKey() {
        new AESEncryptService("").encrypt(TEST_DATA, EncryptionMode.AES_GCM);
    }

}