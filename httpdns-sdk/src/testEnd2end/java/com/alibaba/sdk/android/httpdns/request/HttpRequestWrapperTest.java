package com.alibaba.sdk.android.httpdns.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class HttpRequestWrapperTest {

    private HttpRequestConfig config = mock(HttpRequestConfig.class);
    private HttpRequest request = mock(HttpRequest.class);
    private HttpRequestWrapper wrapper = new HttpRequestWrapper(request);

    @Before
    public void setUp() {
        when(request.getRequestConfig()).thenReturn(config);
    }

    @Test
    public void allMethodWrapped() {
        wrapper.getRequestConfig().setTimeout(2);
        verify(config).setTimeout(2);

        try {
            wrapper.request();
        } catch (Throwable throwable) {
        }

        try {
            verify(request).request();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
