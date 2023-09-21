package com.alibaba.sdk.android.httpdns.probe;

import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.Socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author zonglin.nzl
 * @date 2020/10/23
 */
@RunWith(RobolectricTestRunner.class)
public class ProbeTaskTest {

    private ProbeTask.SpeedTestSocketFactory factory = new ProbeTask.SpeedTestSocketFactory() {
        @Override
        public Socket create() {
            Socket socket = mock(Socket.class);
            UnitTestUtil.setSpeedSort(socket, new String[]{"3.3.3.3", "2.2.2.2", "1.1.1.1"});
            return socket;
        }
    };
    private ProbeCallback callback = mock(ProbeCallback.class);
    private ProbeTask probeTask = new ProbeTask(factory, "www.xxx.com", new String[]{"1.1.1.1", "2.2.2.2", "3.3.3.3"}, new IPProbeItem("www.xxx.com", 80), callback);

    @Test
    public void sortIpsWithSocketConnectSpeed() {
        probeTask.run();
        verify(callback).onResult("www.xxx.com", new String[]{"3.3.3.3", "2.2.2.2", "1.1.1.1"});
    }

}
