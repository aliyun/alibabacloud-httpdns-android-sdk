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
public class HttpRequestWatcherTest {

    private HttpRequestWatcher.Watcher watcher = Mockito.mock(HttpRequestWatcher.Watcher.class);
    private HttpRequest request = Mockito.mock(HttpRequest.class);
    private HttpRequestWatcher httpRequestWatcher = new HttpRequestWatcher(request, watcher);

    @Test
    public void watchSuccess() throws Throwable {
        Mockito.when(request.request()).thenReturn(null);
        HttpRequestConfig requestConfig = new HttpRequestConfig(null, 0, null);
        Mockito.when(request.getRequestConfig()).thenReturn(requestConfig);

        httpRequestWatcher.request();

        Mockito.verify(watcher).onSuccess(requestConfig, null);
        Mockito.verify(watcher, Mockito.never()).onFail(Mockito.eq(requestConfig), Mockito.any(Throwable.class));
    }

    @Test
    public void watchFail() throws Throwable {
        Exception exception = new Exception();
        Mockito.when(request.request()).thenThrow(exception);
        HttpRequestConfig requestConfig = new HttpRequestConfig(null, 0, null);
        Mockito.when(request.getRequestConfig()).thenReturn(requestConfig);

        try {
            httpRequestWatcher.request();
        } catch (Throwable throwable) {
            Mockito.verify(watcher).onFail(requestConfig, exception);
        }
        Mockito.verify(watcher, Mockito.never()).onSuccess(Mockito.eq(requestConfig), Mockito.any(HttpRequestConfig.class));
    }
}
