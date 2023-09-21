package com.alibaba.sdk.android.httpdns.test.utils;

import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

/**
 * @author zonglin.nzl
 * @date 2020/10/20
 */
public class UnitTestUtil {

    public static void assertIpsEmpty(String reason, String[] ips) {
        assertThat(reason, ips.length == 0);
    }

    public static void assertIpsEqual(String reason, String[] ips, String[] serverResponseIps) {
        if (ips == null && serverResponseIps == null) {
            return;
        }

        assertThat(reason, ips, arrayContaining(serverResponseIps));
    }

    public static void setSpeedSort(Socket socket, final String[] ips) {
        try {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object argument = invocation.getArguments()[0];
                    int loopCount = -1;
                    if (argument instanceof InetSocketAddress) {
                        String hostName = ((InetSocketAddress) argument).getAddress().getHostAddress();
                        loopCount = getIndexOfArray(ips, hostName);
                        if (loopCount >= 0) {
                            while (loopCount-- > 0) {
                                Thread.sleep(10);
                            }
                        } else {
                            System.out.println("hostName " + hostName + " loopCount " + loopCount);
                        }
                    } else {
                        System.out.println("argument not inetsocket" + argument);
                    }
                    return null;
                }
            }).when(socket).connect(argThat(new ArgumentMatcher<SocketAddress>() {
                @Override
                public boolean matches(SocketAddress argument) {
                    return true;
                }
            }), anyInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getIndexOfArray(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (target.equals(array[i])) {
                return i;
            }
        }
        return array.length + 1;
    }

    public static String[] changeArraySort(String[] ips) {
        ArrayList<Integer> indexs = new ArrayList<>();
        for (int i = 0; i < ips.length; i++) {
            Integer value = RandomValue.randomInt(ips.length);
            while (indexs.contains(value)) {
                value = RandomValue.randomInt(ips.length);
            }
            indexs.add(value);
        }
        String[] result = new String[ips.length];
        for (int i = 0; i < ips.length; i++) {
            result[i] = ips[indexs.get(i)];
        }
        return result;
    }

    public static <T> ArrayList<T> changeArrayListSort(ArrayList<T> list) {
        for (int i = 0; i < list.size() / 2; i++) {
            int index = RandomValue.randomInt(list.size());
            int swapIndex = RandomValue.randomInt(list.size());
            T t = list.get(index);
            list.set(index, list.get(swapIndex));
            list.set(swapIndex, t);
        }
        return list;
    }

    public static void assertIntArrayEquals(int[] expected, int[] result) {
        if (expected == result) {
            return;
        }
        if (expected == null || result == null || expected.length != result.length) {
            assertThat("int [] not equals", false);
        }
        for (int i = 0; i < expected.length; i++) {
            assertThat("int [] not equals", expected[i] == result[i]);
        }
    }


    public static void testMultiThread(ExecutorService service, final Runnable work, int count) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                    }
                    work.run();
                }
            });
        }
        service.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("start test multiThread");
                countDownLatch.countDown();
            }
        });
    }

    /**
     * 用于测试只能在子线程执行的代码
     * @param work
     * @throws Throwable
     */
    public static void testInSubThread(final Runnable work) throws Throwable {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Throwable[] tr = new Throwable[]{null};
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    work.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                    tr[0] = e;
                }

                countDownLatch.countDown();
            }
        }).start();

        countDownLatch.await();
        if(tr[0] != null) {
            throw tr[0];
        }
    }

}
