package com.alibaba.sdk.android.httpdns.test.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多线程测试辅助类
 * @author zonglin.nzl
 * @date 11/4/21
 */
public class MultiThreadTestHelper {

    /**
     * 开始执行一个多线程测试任务
     * 根据参数 同时启动多个线程同时执行一定时间
     * @param testTask
     */
    public static void start(final TestTask testTask) {
        final CountDownLatch testLatch = new CountDownLatch(testTask.threadCount);
        ExecutorService service = Executors.newFixedThreadPool(testTask.threadCount);
        final CountDownLatch countDownLatch = new CountDownLatch(testTask.threadCount);
        final Throwable[] t = new Throwable[1];
        for (int i = 0; i < testTask.threadCount; i++) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    ThreadTask task = testTask.create();
                    countDownLatch.countDown();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (task != null) {
                        task.prepare();
                        long begin = System.currentTimeMillis();
                        while (System.currentTimeMillis() - begin < testTask.executeTime) {
                            try {
                                task.execute();
                            } catch (Throwable e) {
                                t[0] = e;
                                break;
                            }
                        }
                        task.done();
                    }
                    testLatch.countDown();
                }
            });
        }
        try {
            testLatch.await();
        } catch (InterruptedException e) {
        }
        if (t[0] != null) {
            throw new RuntimeException(t[0]);
        }
        testTask.allFinish();
    }

    /**
     * 单线程的测试任务接口
     */
    public interface ThreadTask {
        /**
         * 执行测试开始前的准备工作
         */
        void prepare();

        /**
         * 执行测试逻辑
         */
        void execute();

        /**
         * 执行测试结果的处理逻辑
         */
        void done();
    }

    /**
     * 测试任务的构造接口 & 测试结束接口
     */
    public interface TaskFactory {
        /**
         * 创建一个测试任务
         * @return
         */
        ThreadTask create();

        /**
         * 所有测试任务完成的回调接口
         */
        void allFinish();
    }

    /**
     * 一个多线程测试任务
     * 需要覆写 create 方法
     * 参数指定 一共多少线程，单个线程执行多长时间
     */
    public static class TestTask implements TaskFactory {
        private int threadCount;
        private long executeTime;

        public TestTask(int threadCount, long executeTime) {
            this.threadCount = threadCount;
            this.executeTime = executeTime;
        }

        @Override
        public ThreadTask create() {
            return null;
        }

        @Override
        public void allFinish() {

        }
    }

    /**
     * 每个线程执行的逻辑一样时的 简单多线程测试
     */
    public static class SimpleTask extends TestTask {
        private Runnable task;

        public SimpleTask(int threadCount, long executeTime, Runnable task) {
            super(threadCount, executeTime);
            this.task = task;
        }

        @Override
        public ThreadTask create() {
            return new ThreadTask() {
                @Override
                public void prepare() {

                }

                @Override
                public void execute() {
                    task.run();
                }

                @Override
                public void done() {

                }
            };
        }
    }
}
