package com.alibaba.sdk.android.httpdns.utils;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import static com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil.assertIpsEqual;

/**
 * @author zonglin.nzl
 * @date 2020/10/23
 */
@RunWith(RobolectricTestRunner.class)
public class CommonUtilTest {


    @Test
    public void sortIpsOrderWithConnectSpeed() {
        String[] sortedIps = CommonUtil.sortIpsWithSpeeds(new String[]{"A", "B", "C"}, new int[]{3, 2, 1});
        assertIpsEqual("速度快的，排前面", sortedIps, new String[]{"C", "B", "A"});

        sortedIps = CommonUtil.sortIpsWithSpeeds(new String[]{"A", "B", "C"}, new int[]{1, 2, 3});
        assertIpsEqual("速度快的，排前面", sortedIps, new String[]{"A", "B", "C"});

        sortedIps = CommonUtil.sortIpsWithSpeeds(new String[]{"A", "B", "C"}, new int[]{2, 3, 1});
        assertIpsEqual("速度快的，排前面", sortedIps, new String[]{"C", "A", "B"});
    }


    @Test
    public void translateStringArrayToString() {
        String[] ips = RandomValue.randomIpv4s();
        String tmp = CommonUtil.translateStringArray(ips);
        String[] ips1 = CommonUtil.parseStringArray(tmp);
        assertIpsEqual("字符串数组转化", ips, ips1);

        MatcherAssert.assertThat("null场景", CommonUtil.translateStringArray(null) == null);
        MatcherAssert.assertThat("null场景", CommonUtil.parseStringArray(null) == null);
        MatcherAssert.assertThat("空场景", CommonUtil.translateStringArray(new String[0]).equals(""));
        MatcherAssert.assertThat("空场景", CommonUtil.parseStringArray("").length == 0);
    }


    @Test
    public void translateIntArrayToString() {
        int[] ports = RandomValue.randomPorts();
        String tmp = CommonUtil.translateIntArray(ports);
        int[] ports1 = CommonUtil.parseIntArray(tmp);
        UnitTestUtil.assertIntArrayEquals(ports, ports1);

        MatcherAssert.assertThat("null场景", CommonUtil.translateIntArray(null) == null);
        MatcherAssert.assertThat("null场景", CommonUtil.parseIntArray(null) == null);
        MatcherAssert.assertThat("空场景", CommonUtil.translateIntArray(new int[0]).equals(""));
        MatcherAssert.assertThat("空场景", CommonUtil.parseIntArray("").length == 0);
    }

    @Test
    public void testToMap() {
        String extra = "{&quot;scale&quot;:&quot;1&quot;,&quot;sdns&quot;:&quot;success&quot;}";
        Map<String, String> map = CommonUtil.toMap(extra);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
}
