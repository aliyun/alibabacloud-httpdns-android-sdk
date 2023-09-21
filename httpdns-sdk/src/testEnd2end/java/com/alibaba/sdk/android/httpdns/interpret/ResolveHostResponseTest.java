package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * 预解析结果
 *
 * @author zonglin.nzl
 * @date 2020/10/20
 */
@RunWith(RobolectricTestRunner.class)
public class ResolveHostResponseTest {

	@Test
	public void validJsonStrTranslateSuccess() throws JSONException {
		String json
			= "{\"dns\":[{\"host\":\"www.taobao.com\",\"client_ip\":\"106.11.34.32\",\"ips\":[\"42"
            + ".81.203.218\",\"42.81.203.219\"],\"type\":1,\"ttl\":32,\"origin_ttl\":60},"
            + "{\"host\":\"www.aliyun.com\",\"client_ip\":\"106.11.34.32\",\"ips\":[\"203.119.207"
            + ".243\"],\"type\":1,\"ttl\":92,\"origin_ttl\":120},{\"host\":\"www.taobao.com\","
            + "\"client_ip\":\"106.11.34.32\",\"ips\":[\"240e:928:501:0:3:0:0:3f0\","
            + "\"240e:928:501:0:3:0:0:3f1\"],\"type\":28,\"ttl\":60,\"origin_ttl\":60},"
            + "{\"host\":\"www.aliyun.com\",\"client_ip\":\"106.11.34.32\",\"ips\":[],"
            + "\"type\":28,\"ttl\":3600}]}";
		ResolveHostResponse response = ResolveHostResponse.fromResponse(json);

		assertThat("解析4个记录", response.getItems().size(), is(4));

		for (ResolveHostResponse.HostItem item : response.getItems()) {
			if (item.getHost().equals("www.taobao.com")) {
				if (item.getIpType() == RequestIpType.v4) {
					UnitTestUtil.assertIpsEqual("解析taobao v4 成功", item.getIps(),
						new String[] {"42.81.203.218", "42.81.203.219"});
				} else {
					UnitTestUtil.assertIpsEqual("解析taobao v6 成功", item.getIps(),
						new String[] {"240e:928:501:0:3:0:0:3f0", "240e:928:501:0:3:0:0:3f1"});
				}
			} else if (item.getHost().equals("www.aliyun.com")) {
				if (item.getIpType() == RequestIpType.v4) {
					UnitTestUtil.assertIpsEqual("解析aliyun  v4 成功", item.getIps(),
						new String[] {"203.119.207.243"});
				} else {
					UnitTestUtil.assertIpsEmpty("解析aliyun  v6 成功", item.getIps());
				}
			} else {
				MatcherAssert.assertThat("解析失败", false);
			}
		}
	}
}
