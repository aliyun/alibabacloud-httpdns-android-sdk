package com.alibaba.sdk.android.httpdns.test.utils;

import org.hamcrest.MatcherAssert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程控制锁
 *
 * @author zonglin.nzl
 * @date 2020/9/3
 */
public class CountUpAndDownLatch {
    private AtomicInteger count;
    private AtomicBoolean countChanged;
    private final Object lock = new Object();
    private boolean checkThreadCount = true;

    public CountUpAndDownLatch(int initCount) {
        this.count = new AtomicInteger(initCount);
        this.countChanged = new AtomicBoolean(true);
    }

    public void countUp() {
        synchronized (lock) {
            this.countChanged.set(true);
            this.count.incrementAndGet();
            this.lock.notifyAll();
        }
    }

    public void countDown() {
        synchronized (lock) {
            this.countChanged.set(true);
            this.count.decrementAndGet();
            this.lock.notifyAll();
        }
    }


    public void countUp(int count) {
        synchronized (lock) {
            this.countChanged.set(true);
            this.count.addAndGet(count);
            this.lock.notifyAll();
        }
    }

    public void countDown(int count) {
        synchronized (lock) {
            this.countChanged.set(true);
            this.count.addAndGet(count * -1);
            this.lock.notifyAll();
        }
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            int retryCount = 3;
            while (countChanged.get() || retryCount > 0) {
                countChanged.set(false);
                try {
                    this.lock.wait(this.count.get() > 0 ? 1000 : 1);
                } catch (Throwable throwable) {
                }
                if (countChanged.get()) {
                    retryCount = 3;
                } else {
                    retryCount--;
                }
            }
            int count = this.count.get();
            if (checkThreadCount) {
                if (count > 0) {
                    MatcherAssert.assertThat("线程使用超出预期 " + count, false);
                }
            } else {
                this.count.set(0);
            }
        }
    }

    public void setCheckThreadCount(boolean checkThreadCount) {
        this.checkThreadCount = checkThreadCount;
    }
}
