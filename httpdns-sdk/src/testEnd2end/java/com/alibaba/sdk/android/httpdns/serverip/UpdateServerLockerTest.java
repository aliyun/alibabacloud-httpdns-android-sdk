package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.test.utils.MultiThreadTestHelper;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zonglin.nzl
 * @date 1/17/22
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateServerLockerTest {


    @Test
    public void onlyOneRequest() {
        UpdateServerLocker locker = new UpdateServerLocker();

        String region = Constants.REGION_MAINLAND;
        MatcherAssert.assertThat("允许第一个请求", locker.begin(region));
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region));

        locker.end(region);

        MatcherAssert.assertThat("第一个请求结束后，允许下次请求", locker.begin(region));
    }

    @Test
    public void requestDifferentRegion() {
        String region = Constants.REGION_MAINLAND;
        String region1 = Constants.REGION_HK;
        UpdateServerLocker locker = new UpdateServerLocker();

        MatcherAssert.assertThat("允许第一个请求", locker.begin(region));
        MatcherAssert.assertThat("允许第一个请求", locker.begin(region1));
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region));
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region1));

        locker.end(region1);
        MatcherAssert.assertThat("第一个请求结束后，允许下次请求", locker.begin(region1));
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region));
    }

    @Test
    public void timeout() throws InterruptedException {
        UpdateServerLocker locker = new UpdateServerLocker(10);
        String region = Constants.REGION_MAINLAND;
        MatcherAssert.assertThat("允许第一个请求", locker.begin(region));
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region));
        Thread.sleep(11);
        // 设计上，我们在超时后第一次判断时，处理超时
        MatcherAssert.assertThat("第一个请求,未完成，其它请求不处理", !locker.begin(region));
        MatcherAssert.assertThat("超时，允许下一个请求", locker.begin(region));
    }


    @Test
    public void multiThread() {
        final String region = Constants.REGION_MAINLAND;
        final String region1 = Constants.REGION_HK;
        final AtomicInteger regionRequesting = new AtomicInteger(0);
        final AtomicInteger region1Requesting = new AtomicInteger(0);
        final UpdateServerLocker locker = new UpdateServerLocker();

        MultiThreadTestHelper.start(new MultiThreadTestHelper.SimpleTask(5, 1000, new Runnable() {
            @Override
            public void run() {
                String tmpRegion = region;
                AtomicInteger tmpRequesting = regionRequesting;
                int random = RandomValue.randomInt(4);
                if (random % 2 == 0) {
                    tmpRegion = region;
                    tmpRequesting = regionRequesting;
                } else if (random % 2 == 1) {
                    tmpRegion = region1;
                    tmpRequesting = region1Requesting;
                }

                if (random < 2) {
                    boolean result = locker.begin(tmpRegion);
                    if (result) {
//                        System.out.println("lock " + tmpRegion + " success");
                        tmpRequesting.incrementAndGet();
                    } else {
//                        System.out.println("lock " + tmpRegion + " fail");
                    }
                } else {
                    if (tmpRequesting.compareAndSet(1, 0)) {
                        locker.end(tmpRegion);
//                        System.out.println("unlock " + tmpRegion + " success");
                    }
                }

                int tmp = tmpRequesting.get();
                MatcherAssert.assertThat("同时只有一个成功", tmp == 0 || tmp == 1);
            }
        }));
    }
}
