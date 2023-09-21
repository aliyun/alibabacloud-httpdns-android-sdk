package com.alibaba.sdk.android.httpdns.test.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * 随机测试数据
 *
 * @author zonglin.nzl
 * @date 2020/9/2
 */
public class RandomValue {
    private static Random random = new Random(System.currentTimeMillis());
    private static char[] chars = new char[52];

    static {
        for (int i = 0; i < 26; i++) {
            chars[i] = (char) ((int) 'a' + i);
            chars[i + 26] = (char) ((int) 'A' + i);
        }
    }

    public static String randomIpv4() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i != 0) {
                stringBuilder.append('.');
            }
            stringBuilder.append("" + randomInt(256));
        }
        return stringBuilder.toString();
    }

    public static String randomIpv6() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i != 0) {
                stringBuilder.append(':');
            }
            stringBuilder.append(randomIpv6Segment());
        }
        return stringBuilder.toString();
    }

    private static String randomIpv6Segment() {
        long value = Math.abs(random.nextLong()) % (0xffff + 1);
        if (value == 0) {
            return "";
        } else {
            return Long.toHexString(value);
        }
    }

    public static String randomHost() {
        int pointsNum = random.nextInt(3) + 2;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < pointsNum; i++) {
            if (i != 0) {
                stringBuilder.append('.');
            }
            stringBuilder.append(randomStringWithMaxLength(30));
        }
        return stringBuilder.toString();
    }

    public static String randomStringWithFixedLength(int length) {
        return randomString(length, 0);
    }

    public static String randomStringWithMaxLength(int max) {
        return randomString(0, max);
    }

    public static String randomString(int length, int maxLength) {
        if (length <= 0) {
            length = randomInt(maxLength <= 0 ? 1024 : maxLength) + 1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(chars[random.nextInt(52)]);
        }
        return stringBuilder.toString();
    }

    public static int randomInt(int range) {
        if (range <= 0) {
            return random.nextInt();
        }
        return random.nextInt(range);
    }

    public static String[] randomIpv4s() {
        return new String[]{
                randomIpv4(),
                randomIpv4(),
                randomIpv4()
        };
    }

    public static String[] randomIpv6s() {
        return new String[]{
                randomIpv6(),
                randomIpv6(),
                randomIpv6()
        };
    }

    public static int[] randomPorts() {
        return new int[]{
                randomInt(40000),
                randomInt(40000),
                randomInt(40000)
        };
    }

    public static String[] randomSort(String[] src) {
        ArrayList<String> list = new ArrayList<>(src.length);
        for (int i = 0; i < src.length; i++) {
            list.add(src[i]);
        }
        String[] randomArray = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            randomArray[i] = list.remove(random.nextInt(list.size()));
        }
        return randomArray;
    }

    public static String randomJsonMap() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(RandomValue.randomStringWithFixedLength(8), RandomValue.randomStringWithFixedLength(8));
            jsonObject.put(RandomValue.randomStringWithFixedLength(8), RandomValue.randomStringWithFixedLength(8));
            jsonObject.put(RandomValue.randomStringWithFixedLength(8), RandomValue.randomStringWithFixedLength(8));
        } catch (JSONException e) {
        }
        return jsonObject.toString();
    }
}
