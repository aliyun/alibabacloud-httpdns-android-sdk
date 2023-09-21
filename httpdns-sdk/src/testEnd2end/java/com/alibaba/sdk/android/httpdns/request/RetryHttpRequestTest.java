package com.alibaba.sdk.android.httpdns.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class RetryHttpRequestTest {
    private HttpRequest request = Mockito.mock(HttpRequest.class);

    @Test
    public void retryRequestWhenFail() throws Throwable {
        RetryHttpRequest retryHttpRequest = new RetryHttpRequest(request, 2);
        Mockito.when(request.request()).thenThrow(new Exception());

        try {
            retryHttpRequest.request();
        } catch (Throwable throwable) {

        }
        Mockito.verify(request, Mockito.times(3)).request();
    }

    @Test
    public void noRetryRequestWhenSuccess() throws Throwable {
        RetryHttpRequest retryHttpRequest = new RetryHttpRequest(request, 3);
        Mockito.when(request.request()).thenThrow(new Exception()).thenReturn(null);

        try {
            retryHttpRequest.request();
        } catch (Throwable throwable) {
        }

        Mockito.verify(request, Mockito.times(2)).request();
    }


}
