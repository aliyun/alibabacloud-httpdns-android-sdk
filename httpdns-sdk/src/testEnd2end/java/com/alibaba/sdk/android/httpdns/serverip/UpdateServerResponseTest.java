package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.test.server.ServerIpsServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

/**
 * @author zonglin.nzl
 * @date 2020/10/19
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateServerResponseTest {

	@Test
	public void createFromJson() throws JSONException {
		String[] ips = RandomValue.randomIpv4s();
		String[] ipv6s = RandomValue.randomIpv6s();
		int[] ports = RandomValue.randomPorts();

		UpdateRegionServerResponse response = UpdateRegionServerResponse.fromResponse(
			ServerIpsServer.createUpdateServerResponse(ips, ipv6s, ports, ports));

		assertThat(response.getServerIps(), arrayContaining(ips));
		assertThat(response.getServerIpv6s(), arrayContaining(ipv6s));
		UnitTestUtil.assertIntArrayEquals(response.getServerPorts(), ports);
		UnitTestUtil.assertIntArrayEquals(response.getServerIpv6Ports(), ports);

		response = UpdateRegionServerResponse.fromResponse(
			ServerIpsServer.createUpdateServerResponse(ips, ipv6s, null, null));

		assertThat(response.getServerIps(), arrayContaining(ips));
		assertThat(response.getServerIpv6s(), arrayContaining(ipv6s));
		UnitTestUtil.assertIntArrayEquals(response.getServerPorts(), null);
		UnitTestUtil.assertIntArrayEquals(response.getServerIpv6Ports(), null);
	}

	@Test
	public void invalidJsonStrWillThrowException() {
		String invalidJson = "{";
		try {
			UpdateRegionServerResponse ignored = UpdateRegionServerResponse.fromResponse(invalidJson);
			assertThat("should throw exception", false);
		} catch (Throwable throwable) {
			// ignored
		}
	}
}
