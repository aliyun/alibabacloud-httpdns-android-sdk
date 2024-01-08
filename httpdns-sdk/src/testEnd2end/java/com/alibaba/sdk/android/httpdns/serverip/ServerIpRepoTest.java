package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * @author zonglin.nzl
 * @date 2020/12/4
 */
@RunWith(RobolectricTestRunner.class)
public class ServerIpRepoTest {

    private RegionServerIpRepo repo = new RegionServerIpRepo();

    @Test
    public void saveAndGet() {
        MatcherAssert.assertThat("没有数据时应该返回null", repo.getServerIps(null) == null);
        MatcherAssert.assertThat("没有数据时应该返回null", repo.getPorts(null) == null);
        MatcherAssert.assertThat("没有数据时应该返回null", repo.getServerIps("") == null);
        MatcherAssert.assertThat("没有数据时应该返回null", repo.getPorts("") == null);

        String hk = Constants.REGION_HK;
        String[] hkIps = RandomValue.randomIpv4s();
        repo.save(hk, hkIps, null, null, null);

        MatcherAssert.assertThat("应该可以正确获取存入的值", repo.getServerIps(hk), Matchers.arrayContaining(hkIps));
        MatcherAssert.assertThat("port没有设置就是null", repo.getPorts(hk) == null);

        String sg = "sg";
        String[] sgIps = RandomValue.randomIpv4s();
        int[] sgPorts = RandomValue.randomPorts();
        repo.save(sg, sgIps, sgPorts, null, null);

        MatcherAssert.assertThat("应该可以正确获取存入的值", repo.getServerIps(sg), Matchers.arrayContaining(sgIps));
        UnitTestUtil.assertIntArrayEquals(sgPorts, repo.getPorts(sg));
    }

    @Test
    public void timeLimit() throws InterruptedException {
        String sg = "sg";
        String[] sgIps = RandomValue.randomIpv4s();
        int[] sgPorts = RandomValue.randomPorts();
        repo.save(sg, sgIps, sgPorts, null, null);

        MatcherAssert.assertThat("应该可以正确获取存入的值", repo.getServerIps(sg), Matchers.arrayContaining(sgIps));
        UnitTestUtil.assertIntArrayEquals(sgPorts, repo.getPorts(sg));

        Thread.sleep(1000);
        MatcherAssert.assertThat("默认有效期是5分钟，当前应该可以获取到数据", repo.getServerIps(sg), Matchers.arrayContaining(sgIps));
        UnitTestUtil.assertIntArrayEquals(sgPorts, repo.getPorts(sg));
        repo.setTimeInterval(500);
        MatcherAssert.assertThat("超过有效期，应该返回null", repo.getServerIps(sg) == null);
        MatcherAssert.assertThat("超过有效期，应该返回null", repo.getPorts(sg) == null);
    }
}
