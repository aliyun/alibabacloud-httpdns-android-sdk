package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
@RunWith(RobolectricTestRunner.class)
public class HostResolveRecorderTest {
    private HostResolveRecorder recorder = new HostResolveRecorder();

    @Test
    public void canNotBeginResolveSameHostTwice() {
        String host = RandomValue.randomHost();

        MatcherAssert.assertThat("host没有解析时，可以成功开始解析", recorder.beginResolve(host, RequestIpType.v4));
        MatcherAssert.assertThat("host解析时，不可以再解析", !recorder.beginResolve(host, RequestIpType.v4));
        recorder.endResolve(host, RequestIpType.v4);
        MatcherAssert.assertThat("host解析完之后，可以再次开始解析", recorder.beginResolve(host, RequestIpType.v4));
        MatcherAssert.assertThat("可以同时解析v4 v6", recorder.beginResolve(host, RequestIpType.v6));
        MatcherAssert.assertThat("解析v4 v6时，不可以再次解析v4 v6", !recorder.beginResolve(host, RequestIpType.both));
        recorder.endResolve(host, RequestIpType.v6);
        MatcherAssert.assertThat("解析v4时，可以再一起解析v4 v6", recorder.beginResolve(host, RequestIpType.both));
        recorder.endResolve(host, RequestIpType.v4);
        MatcherAssert.assertThat("一起解析时，不可以再解析v4", !recorder.beginResolve(host, RequestIpType.v4));
        MatcherAssert.assertThat("一起解析时，不可以再解析v6", !recorder.beginResolve(host, RequestIpType.v6));
        recorder.endResolve(host, RequestIpType.both);
        MatcherAssert.assertThat("host解析完之后，可以再次开始解析", recorder.beginResolve(host, RequestIpType.v6));
        recorder.endResolve(host, RequestIpType.v6);

    }

    @Test
    public void differentCacheKeyIsDifferentStatus() {
        String host = RandomValue.randomHost();
        String cacheKey1 = null;
        String cacheKey2 = RandomValue.randomStringWithFixedLength(8);
        String cacheKey3 = RandomValue.randomStringWithFixedLength(8);
        MatcherAssert.assertThat("host没有解析时，可以成功开始解析", recorder.beginResolve(host, RequestIpType.v4, cacheKey1));
        MatcherAssert.assertThat("有和没有cacheKey的解析状态单独计算", recorder.beginResolve(host, RequestIpType.v4, cacheKey2));
        MatcherAssert.assertThat("不同cacheKey的解析状态单独计算", recorder.beginResolve(host, RequestIpType.v4, cacheKey3));
        MatcherAssert.assertThat("默认cacheKey为null", !recorder.beginResolve(host, RequestIpType.v4));
        MatcherAssert.assertThat("相同cacheKey，v4 v6状态单独计算", recorder.beginResolve(host, RequestIpType.v6, cacheKey2));
        recorder.endResolve(host, RequestIpType.v4, cacheKey2);
        MatcherAssert.assertThat("其它和没有cacheKey时逻辑一致", recorder.beginResolve(host, RequestIpType.both, cacheKey2));
    }

    @Test
    public void testMultiThread() {
        final TestExecutorService worker = new TestExecutorService(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        }));

        final ArrayList<String> hosts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hosts.add(RandomValue.randomHost());
        }
        UnitTestUtil.testMultiThread(worker, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    final String host = hosts.get(RandomValue.randomInt(5));
                    final RequestIpType type = RequestIpType.values()[RandomValue.randomInt(3)];
                    worker.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean result = recorder.beginResolve(host, type);
                            if (result) {
                                worker.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        recorder.endResolve(host, type);
                                    }
                                });
                            }
                        }
                    });
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 8);

        try {
            worker.await();
        } catch (InterruptedException e) {
        }

        for (String host : hosts) {
            MatcherAssert.assertThat("状态应该保持一致，最终可以再次解析", recorder.beginResolve(host, RequestIpType.v4));
            MatcherAssert.assertThat("状态应该保持一致，最终可以再次解析", recorder.beginResolve(host, RequestIpType.v6));
        }
    }
}
