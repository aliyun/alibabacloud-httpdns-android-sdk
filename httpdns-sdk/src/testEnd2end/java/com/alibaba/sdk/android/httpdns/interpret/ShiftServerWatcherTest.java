package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.config.ServerConfig;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.net.SocketTimeoutException;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class ShiftServerWatcherTest {

    private ScheduleService scheduleService = Mockito.mock(ScheduleService.class);
    private HttpRequestConfig requestConfig = Mockito.mock(HttpRequestConfig.class);
    private StatusControl statusControl = Mockito.mock(StatusControl.class);
    private HttpDnsConfig config = Mockito.mock(HttpDnsConfig.class);
    private ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
    private ShiftServerWatcher watcher = new ShiftServerWatcher(config, scheduleService, statusControl);
    private String ip = RandomValue.randomIpv4();
    private int port = RandomValue.randomInt(40000);

    @Before
    public void setUp() {
        Mockito.when(requestConfig.getIp()).thenReturn(ip);
        Mockito.when(requestConfig.getPort()).thenReturn(port);
        Mockito.when(config.getCurrentServer()).thenReturn(serverConfig);
        Mockito.when(serverConfig.getServerIp()).thenReturn(ip);
        Mockito.when(serverConfig.getPort()).thenReturn(port);
        Mockito.when(config.getCurrentServer().markOkServer(ip, port)).thenReturn(true);
    }

    @Test
    public void requestSuccessWillMarkCurrentServerAndTurnUpStatus() {
        watcher.onSuccess(requestConfig, null);
        Mockito.verify(serverConfig).markOkServer(ip, port);
        Mockito.verify(statusControl).turnUp();
        Mockito.verify(statusControl, Mockito.never()).turnDown();
    }

    @Test
    public void requestSuccessWillMarkCurrentServerAndIfMarkFailWillNotChangeStatus() {
        Mockito.when(serverConfig.markOkServer(ip, port)).thenReturn(false);
        watcher.onSuccess(requestConfig, null);
        Mockito.verify(statusControl, Mockito.never()).turnUp();
        Mockito.verify(statusControl, Mockito.never()).turnDown();
    }

    @Test
    public void shiftServerWhenServerNotAvailableAndTurnDownStatus() {
        watcher.onFail(requestConfig, new SocketTimeoutException());
        Mockito.verify(serverConfig).shiftServer(ip, port);
        Mockito.verify(requestConfig).setIp(ip);
        Mockito.verify(requestConfig).setPort(port);
        Mockito.verify(statusControl).turnDown();
        Mockito.verify(statusControl, Mockito.never()).turnUp();
    }

    @Test
    public void shiftServerWhenServerDegradeAndTurnDownStatus() {
        watcher.onFail(requestConfig, HttpException.create(HttpException.ERROR_CODE_403, HttpException.ERROR_MSG_SERVICE_LEVEL_DENY));
        Mockito.verify(serverConfig).shiftServer(ip, port);
        Mockito.verify(requestConfig).setIp(ip);
        Mockito.verify(requestConfig).setPort(port);
        Mockito.verify(statusControl).turnDown();
        Mockito.verify(statusControl, Mockito.never()).turnUp();
    }

    @Test
    public void doShiftServerWhenThrowOtherError() throws Throwable {
        Exception exception = new Exception();
        watcher.onStart(requestConfig);
        watcher.onFail(requestConfig, exception);
        Mockito.verify(serverConfig).shiftServer(ip, port);
        Mockito.verify(requestConfig).setIp(ip);
        Mockito.verify(requestConfig).setPort(port);
        Mockito.verify(statusControl, Mockito.never()).turnUp();
        Mockito.verify(statusControl).turnDown();
    }

    @Test
    public void shiftServerWhenFailCostMoreThanTimeout() throws Throwable {
        requestConfig.setTimeout(299);
        Exception exception = new Exception();
        watcher.onStart(requestConfig);
        Thread.sleep(300);
        watcher.onFail(requestConfig, exception);
        Mockito.verify(serverConfig).shiftServer(ip, port);
        Mockito.verify(requestConfig).setIp(ip);
        Mockito.verify(requestConfig).setPort(port);
        Mockito.verify(statusControl).turnDown();
        Mockito.verify(statusControl, Mockito.never()).turnUp();
    }


    @Test
    public void whenAllServerUsedUpdateServerIps() {
        Mockito.when(serverConfig.shiftServer(ip, port)).thenReturn(true);
        watcher.onFail(requestConfig, new SocketTimeoutException());
        Mockito.verify(scheduleService).updateServerIps();
    }

}
