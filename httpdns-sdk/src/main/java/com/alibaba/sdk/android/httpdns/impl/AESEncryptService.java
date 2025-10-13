package com.alibaba.sdk.android.httpdns.impl;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES 加密解密工具类
 * @author renwei
 * @date 2025/3/26
 */
public class AESEncryptService {
    private static final int GCM_IV_LENGTH = 12;
    private static final int CBC_IV_LENGTH = 16;
    private static final int GCM_TAG_LENGTH = 128;

    private String aesSecretKey;

    /**
     * 加密方法
     */
    public String encrypt(String data, EncryptionMode mode) {
        if (EncryptionMode.PLAIN == mode) {
            return "";
        }
        if (TextUtils.isEmpty(aesSecretKey)) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("aesSecretKey为空");
            }
            return "";
        }
        if (TextUtils.isEmpty(data)) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("待加密数据为空");
            }
            return "";
        }

        // 生成IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[mode == EncryptionMode.AES_GCM ? GCM_IV_LENGTH : CBC_IV_LENGTH];
        random.nextBytes(iv);

        String encryptStr = "";
        try {
            byte[] encrypted = mode == EncryptionMode.AES_GCM ? aesGcmEncrypt(data, iv) : aesCbcEncrypt(data, iv);
            // 组合IV和密文
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            encryptStr = CommonUtil.encodeHexString(combined);
        } catch (Exception e) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.e("加密失败, 加密内容:" + data);
            }
        }
        return encryptStr;
    }

    private byte[] aesGcmEncrypt(String plainText, byte[] iv) throws Exception {
        byte[]  key = CommonUtil.decodeHex(aesSecretKey);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv); // 认证标签长度为 128 位
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] aesCbcEncrypt(String plainText, byte[] iv) throws Exception {
        byte[]  key = CommonUtil.decodeHex(aesSecretKey);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // IV 必须为 16 字节
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密方法
     */
    public String decrypt(String encryptData, EncryptionMode mode) {
        if (EncryptionMode.PLAIN == mode) {
            return encryptData;
        }
        if (TextUtils.isEmpty(aesSecretKey)) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("aesSecretKey为空");
            }
            return "";
        }
        if (TextUtils.isEmpty(encryptData)) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("待解密数据为空");
            }
            return "";
        }
        String plainData = "";
        try {
            byte[] binaryencrypted = CommonUtil.decodeBase64(encryptData);
            plainData = EncryptionMode.AES_GCM == mode ? aesGcmDecrypt(binaryencrypted): aesCbcDecrypt(binaryencrypted);
        } catch (Exception e) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.e("解密失败, 待解密数据: " + encryptData);
            }
        }
        return plainData;
    }

    private String aesCbcDecrypt(byte[] binaryencrypted) throws Exception {
        byte[]  key = CommonUtil.decodeHex(aesSecretKey);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(binaryencrypted, 0, 16));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(Arrays.copyOfRange(binaryencrypted, 16, binaryencrypted.length));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String aesGcmDecrypt(byte[] binaryencrypted) throws Exception {
        byte[]  key = CommonUtil.decodeHex(aesSecretKey);
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, Arrays.copyOfRange(binaryencrypted, 0, 12)); // 认证标签长度为 128 位
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] decrypted = cipher.doFinal(Arrays.copyOfRange(binaryencrypted, 12, binaryencrypted.length));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public Boolean isEncryptionMode(){
        return !TextUtils.isEmpty(aesSecretKey);
    }

    public void setAesSecretKey(String aesSecretKey) {
        this.aesSecretKey = aesSecretKey;
    }

    public enum EncryptionMode {
        PLAIN("0"), AES_CBC("1"), AES_GCM("2");

        private final String mode;

        EncryptionMode(String mode) {
            this.mode = mode;
        }

        public String getMode(){
            return mode;
        }

    }

}
