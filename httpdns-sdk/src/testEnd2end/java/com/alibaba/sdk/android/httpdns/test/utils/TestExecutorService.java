package com.alibaba.sdk.android.httpdns.test.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试使用的 线程池
 * 可以进行异步控制，等待异步逻辑处理完成
 * @author zonglin.nzl
 * @date 2020/9/3
 */
public class TestExecutorService implements ScheduledExecutorService {

    private CountUpAndDownLatch latch = new CountUpAndDownLatch(0);
    private ScheduledExecutorService originService;

    public TestExecutorService(final ExecutorService executorService) {
        this.originService = new ScheduledExecutorService() {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                return null;
            }

            @Override
            public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                return null;
            }

            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                return null;
            }

            @Override
            public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                return null;
            }

            @Override
            public void shutdown() {
                executorService.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return executorService.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return executorService.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return executorService.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return executorService.awaitTermination(timeout, unit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return executorService.submit(task);
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return executorService.submit(task, result);
            }

            @Override
            public Future<?> submit(Runnable task) {
                return executorService.submit(task);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return executorService.invokeAll(tasks);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return executorService.invokeAll(tasks, timeout, unit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
                return executorService.invokeAny(tasks);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                return executorService.invokeAny(tasks, timeout, unit);
            }

            @Override
            public void execute(Runnable command) {
                executorService.execute(command);
            }
        };
    }

    public TestExecutorService(ScheduledExecutorService originService) {
        this.originService = originService;
    }

    public void enableThreadCountCheck(boolean check) {
        this.latch.setCheckThreadCount(check);
    }

    public void await() throws InterruptedException {
        this.latch.await();
    }

    @Override
    public void shutdown() {
        this.originService.shutdown();
    }


    @Override
    public List<Runnable> shutdownNow() {
        return this.originService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.originService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.originService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.originService.awaitTermination(timeout, unit);
    }


    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        latch.countUp();
        try {
            return this.originService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try {
                        T t = task.call();
                        return t;
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public <T> Future<T> submit(final Runnable task, T result) {
        latch.countUp();
        try {
            return this.originService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            }, result);
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public Future<?> submit(final Runnable task) {
        latch.countUp();
        try {
            return this.originService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        final int count = tasks != null ? tasks.size() : 0;
        latch.countUp(count);
        try {
            return this.originService.invokeAll(wrap(tasks, this.latch));
        } catch (Throwable throwable) {
            latch.countDown(count);
            throw throwable;
        }
    }

    private static <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks, final CountUpAndDownLatch latch) {
        if (tasks == null) {
            return null;
        }
        ArrayList<Callable<T>> list = new ArrayList<>();
        Iterator<? extends Callable<T>> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            final Callable<T> callable = iterator.next();
            list.add(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try {
                        return callable.call();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        return list;
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        final int count = tasks != null ? tasks.size() : 0;
        latch.countUp(count);
        try {
            return this.originService.invokeAll(wrap(tasks, this.latch), timeout, unit);
        } catch (Throwable throwable) {
            latch.countDown(count);
            throw throwable;
        }
    }


    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return this.originService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return this.originService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        latch.countUp();
        try {
            this.originService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public ScheduledFuture<?> schedule(final Runnable command, long delay, TimeUnit unit) {
        latch.countUp();
        try {
            return this.originService.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            }, delay, unit);
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, long delay, TimeUnit unit) {
        latch.countUp();
        try {
            return this.originService.schedule(new Callable<V>() {
                @Override
                public V call() throws Exception {
                    try {
                        return callable.call();
                    } catch (Throwable throwable) {
                        throw throwable;
                    } finally {
                        latch.countDown();
                    }
                }
            }, delay, unit);
        } catch (Throwable throwable) {
            latch.countDown();
            throw throwable;
        }
    }


    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return this.originService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }


    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return this.originService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
