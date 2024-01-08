package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author zonglin.nzl
 * @date 2020/10/20
 */
@RunWith(RobolectricTestRunner.class)
public class HttpRequestTest {

    private HttpDnsServer server = new HttpDnsServer();
    private HttpRequest<String> httpRequest;

    @Before
    public void setup() {
        server.start();
        HttpRequestConfig requestConfig = new HttpRequestConfig(server.getServerIp(), server.getPort(), "/debug");
        requestConfig.setTimeout(2000);
        httpRequest = new HttpRequest<>(requestConfig, new ResponseParser<String>() {
            @Override
            public String parse(String response) throws Throwable {
                return response;
            }
        });
    }

    @After
    public void tearDown() {
        server.stop();
        httpRequest = null;
    }

    @Test
    public void sendRequestAndGetResponse() {
        String response = "aaaaabbbbbbcccccc";
        server.getDebugApiServer().preSetRequestResponse(null, 200, response, -1);
        try {
            String res = httpRequest.request();
            assertThat(res, equalTo(response));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            assertThat("should not fail", false);
        }
    }

    @Test
    public void sendUpdateServerRequestAndServerNotAvailable() {
        server.getDebugApiServer().preSetRequestResponse(null, 403, "whatever", -1);
        try {
            String res = httpRequest.request();
            assertThat("should not success", false);
        } catch (Throwable throwable) {
            assertThat("throwable is httpexception", throwable instanceof HttpException);
            assertThat((HttpException) throwable, is(equalTo(HttpException.create(403, "whatever"))));
        }
    }

    @Test
    public void sendUpdateServerRequestAndServerNotReachable() {
        server.getDebugApiServer().preSetRequestTimeout(null, -1);
        try {
            String res = httpRequest.request();
            assertThat("should not success", false);
        } catch (Throwable throwable) {
            assertThat("request is timeout", true);
        }
    }

}
