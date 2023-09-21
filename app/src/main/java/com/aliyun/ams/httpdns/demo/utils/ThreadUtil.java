package com.aliyun.ams.httpdns.demo.utils;

import android.util.Log;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.aliyun.ams.httpdns.demo.MyApp;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zonglin.nzl
 * @date 8/31/22
 */
public class ThreadUtil {

    public static void multiThreadTest(final String[] validHosts, final int hostCount, final int threadCount, final int executeTime, final boolean async, final RequestIpType type) {
        int validCount = validHosts.length;
        if (validCount > hostCount) {
            validCount = hostCount;
        }
        final int tmpValidCount = validCount;
        Log.d(MyApp.TAG, threadCount + "线程并发，执行" + executeTime + ", 总域名" + hostCount + "个，有效" + validCount + "个");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> hosts = new ArrayList<>(hostCount);
                for (int i = 0; i < hostCount - tmpValidCount; i++) {
                    hosts.add("test" + i + ".aliyun.com");
                }
                for (int i = 0; i < tmpValidCount; i++) {
                    hosts.add(validHosts[i]);
                }

                final CountDownLatch testLatch = new CountDownLatch(threadCount);
                ExecutorService service = Executors.newFixedThreadPool(threadCount);
                final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
                for (int i = 0; i < threadCount; i++) {
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                            countDownLatch.countDown();
                            try {
                                countDownLatch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Random random = new Random(Thread.currentThread().getId());
                            int allRequestCount = 0;
                            int slowRequestCount = 0;
                            int emptyResponseCount = 0;
                            long maxSlowRequestTime = 0;
                            long subSlowRequestTime = 0;
                            int taobaoCount = 0;
                            int taobaoSuccessCount = 0;
                            long firstTaobaoTime = 0;
                            long firstTaobaoSuccessTime = 0;
                            long begin = System.currentTimeMillis();
                            while (System.currentTimeMillis() - begin < executeTime) {
                                String host = hosts.get(random.nextInt(hostCount));
                                long startRequestTime = System.currentTimeMillis();
                                HTTPDNSResult ips = null;
                                if (async) {
                                    ips = MyApp.getInstance().getService().getIpsByHostAsync(host, type);
                                } else {
                                    ips = ((SyncService) MyApp.getInstance().getService()).getByHost(host, type);
                                }
                                long endRequestTime = System.currentTimeMillis();
                                if (host.equals("www.taobao.com")) {
                                    if (taobaoCount == 0) {
                                        firstTaobaoTime = System.currentTimeMillis();
                                    }
                                    taobaoCount++;
                                    if (ips.getIps() != null && ips.getIps().length > 0) {
                                        if (taobaoSuccessCount == 0) {
                                            firstTaobaoSuccessTime = System.currentTimeMillis();
                                        }
                                        taobaoSuccessCount++;
                                    }
                                }
                                if (endRequestTime - startRequestTime > 100) {
                                    slowRequestCount++;
                                    subSlowRequestTime += endRequestTime - startRequestTime;
                                    if (maxSlowRequestTime < endRequestTime - startRequestTime) {
                                        maxSlowRequestTime = endRequestTime - startRequestTime;
                                    }
                                }
                                if (ips == null || ips.getIps() == null || ips.getIps().length == 0) {
                                    emptyResponseCount++;
                                }
                                allRequestCount++;
                            }

                            String msg = Thread.currentThread().getId() + " allRequestCount: " + allRequestCount + ", slowRequestCount: " + slowRequestCount + ", emptyResponseCount: " + emptyResponseCount
                                    + ", maxSlowRequestTime : " + maxSlowRequestTime + ", avgSlowRequestTime: " + (slowRequestCount == 0 ? 0 : subSlowRequestTime / slowRequestCount)
                                    + ", taoRequestCount: " + taobaoCount + ", taoSuccessRequestCount: " + taobaoSuccessCount + ", firstTaoRequestTime: " + (firstTaobaoTime - begin) + ", firstSuccessTaoRequestTime: " + (firstTaobaoSuccessTime - begin);
                            Log.w(MyApp.TAG, "asyncMulti " + msg);
                            testLatch.countDown();
                        }
                    });
                }

                try {
                    testLatch.await();
                } catch (InterruptedException e) {
                }
            }
        }).start();
    }
}
