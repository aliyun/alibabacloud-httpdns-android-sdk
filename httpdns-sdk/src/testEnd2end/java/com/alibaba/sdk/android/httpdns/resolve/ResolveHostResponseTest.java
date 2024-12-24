package com.alibaba.sdk.android.httpdns.resolve;

import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author zonglin.nzl
 * @date 2020/10/20
 */
@RunWith(RobolectricTestRunner.class)
public class ResolveHostResponseTest {

    @Test
    public void validJsonStrTranslateSuccess() throws JSONException {
        String emptyJson = "{\"host\":\"www.InterpreHostTaskTest.com\",\"ttl\":60}";
        ResolveHostResponse emptyResponse = ResolveHostResponse.fromResponse(emptyJson);

        assertThat(emptyResponse.getIps(), Matchers.<String>arrayWithSize(0));

        String alljson = "{\"host\":\"www.InterpreHostTaskTest.com\",\"ips\":[\"71.60.244.127\",\"63.21.70.243\",\"44.21.106.160\"],\"ipsv6\":[\"e9f3:55a2:ce4:e1e9:f575:5d5c:cb64:468\",\"87db:601:e127:15a:feb9:18fa:2e53:180c\",\"e3c2:751:d55d:53f:37d3:925b:47e7:b025\"],\"ttl\":60,\"extra\":\"extraValue\",\"origin_ttl\":60,\"client_ip\":\"106.11.41.215\"}";
        ResolveHostResponse allReponse = ResolveHostResponse.fromResponse(alljson);

        assertThat(allReponse.getHostName(), is(equalTo("www.InterpreHostTaskTest.com")));
        assertThat(allReponse.getIps(), arrayContaining(new String[]{"71.60.244.127", "63.21.70.243", "44.21.106.160"}));
        assertThat(allReponse.getIpsV6(), arrayContaining(new String[]{"e9f3:55a2:ce4:e1e9:f575:5d5c:cb64:468", "87db:601:e127:15a:feb9:18fa:2e53:180c", "e3c2:751:d55d:53f:37d3:925b:47e7:b025"}));
        assertThat(allReponse.getTtl(), is(equalTo(60)));
        assertThat(allReponse.getExtras(), is(equalTo("extraValue")));
    }

    @Test
    public void invalidJsonStrWillThrowException() {
        String noHostJson = "{\"ttl\":60}";
        try {
            ResolveHostResponse ignored = ResolveHostResponse.fromResponse(noHostJson);
            assertThat("should throw exception", false);
        } catch (Throwable throwable) {
            // ignored
        }

        String noTtlJson = "{\"host\":\"www.InterpreHostTaskTest.com\"}";
        try {
            ResolveHostResponse ignored = ResolveHostResponse.fromResponse(noTtlJson);
            assertThat("should throw exception", false);
        } catch (Throwable throwable) {
            // ignored
        }

        String invalidJson = "}";
        try {
            ResolveHostResponse ignored = ResolveHostResponse.fromResponse(invalidJson);
            assertThat("should throw exception", false);
        } catch (Throwable throwable) {
            // ignored
        }
    }
}
