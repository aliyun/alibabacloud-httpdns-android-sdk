package com.alibaba.sdk.android.httpdns.ranking;

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
public class IPRankingTaskTest {

	private IPRankingTask.SpeedTestSocketFactory factory
		= new IPRankingTask.SpeedTestSocketFactory() {
		@Override
		public Socket create() {
			Socket socket = mock(Socket.class);
			UnitTestUtil.setSpeedSort(socket, new String[] {"3.3.3.3", "2.2.2.2", "1.1.1.1"});
			return socket;
		}
	};
	private IPRankingCallback callback = mock(IPRankingCallback.class);
	private IPRankingTask rankingTask = new IPRankingTask(factory, "www.xxx.com",
		new String[] {"1.1.1.1", "2.2.2.2", "3.3.3.3"}, new IPRankingBean("www.xxx.com", 80),
		callback);

	@Test
	public void sortIpsWithSocketConnectSpeed() {
		rankingTask.run();
		verify(callback).onResult("www.xxx.com", new String[] {"3.3.3.3", "2.2.2.2", "1.1.1.1"});
	}

}
