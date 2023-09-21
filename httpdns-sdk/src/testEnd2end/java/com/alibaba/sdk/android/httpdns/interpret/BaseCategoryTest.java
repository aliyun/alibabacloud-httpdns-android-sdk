package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 域名解析策略相同部分的测试
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
@RunWith(RobolectricTestRunner.class)
public class BaseCategoryTest {

    protected HttpDnsServer server = new HttpDnsServer();

    protected HttpDnsConfig httpDnsConfig;
    protected String host = RandomValue.randomHost();
    protected RequestCallback<InterpretHostResponse> callback = mock(RequestCallback.class);

    protected ScheduleService scheduleService = mock(ScheduleService.class);
    protected StatusControl statusControl = mock(StatusControl.class);
    protected TestExecutorService testExecutorService = new TestExecutorService(new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "httpdnstest");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    }));

    private NormalCategory category = new NormalCategory(scheduleService, statusControl);
    private ILogger logger;

    protected InterpretHostCategory getCategory() {
        return category;
    }

    @Before
    public void setUp() {
        server.start();
        logger = new ILogger() {
            @Override
            public void log(String msg) {
                System.out.println("[Httpdns]" + msg);
            }
        };
        HttpDnsLog.setLogger(logger);
        httpDnsConfig = new HttpDnsConfig(RuntimeEnvironment.application, "10000");
        httpDnsConfig.setInitServers(Constants.REGION_MAINLAND, new String[]{server.getServerIp()}, new int[]{server.getPort()}, null, null);
        httpDnsConfig.setTimeout(1000);
        httpDnsConfig.setWorker(testExecutorService);
    }

    @After
    public void tearDown() {
        HttpDnsLog.removeLogger(logger);
        server.stop();
    }

    @Test
    public void interpretHostSuccessWithIps() {
        InterpretHostResponse interpretHostResponse = new InterpretHostResponse(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), 60, RandomValue.randomJsonMap());
        server.getInterpretHostServer().preSetRequestResponse(host, interpretHostResponse, -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback).onSuccess(interpretHostResponse);
        verify(callback, never()).onFail(any(Throwable.class));
    }

    @Test
    public void interpretHostSuccessWithEmpty() {
        InterpretHostResponse interpretHostResponse = new InterpretHostResponse(host, new String[0], null, 60, null);
        server.getInterpretHostServer().preSetRequestResponse(host, interpretHostResponse, -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback).onSuccess(interpretHostResponse);
        verify(callback, never()).onFail(any(Throwable.class));
    }

    @Test
    public void interpreHostFailWhenServerIsNotReachable() {
        server.getInterpretHostServer().preSetRequestTimeout(host, -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback, never()).onSuccess(any(InterpretHostResponse.class));
        verify(callback).onFail(any(Throwable.class));
    }

    @Test
    public void interpreHostFailWhenServerIsNotAvailable() {
        server.getInterpretHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback, never()).onSuccess(any(InterpretHostResponse.class));
        verify(callback).onFail(argThat(match(403, "whatever")));
    }


    @Test
    public void changeServerIpWhenServerIsNotAvailable() {
        server.getInterpretHostServer().preSetRequestResponse(host, HttpException.ERROR_CODE_403, HttpException.ERROR_MSG_SERVICE_LEVEL_DENY, -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MatcherAssert.assertThat("when server is not availabe, change server ip", server.getInterpretHostServer().hasRequestForArg(host, -1, false));
    }

    @Test
    public void interpretHostSuccessWillTurnUpStatus() {
        InterpretHostResponse interpretHostResponse = new InterpretHostResponse(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), 60, RandomValue.randomJsonMap());
        server.getInterpretHostServer().preSetRequestResponse(host, interpretHostResponse, -1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(statusControl).turnUp();
    }

    @Test
    public void interpreHostFailWhenServerIsNotReachableWillTurnDownStatus() {
        server.getInterpretHostServer().preSetRequestResponse(host, HttpException.ERROR_CODE_403, HttpException.ERROR_MSG_SERVICE_LEVEL_DENY, 1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(statusControl).turnDown();
    }

    @Test
    public void whenAllServerIpUsedWillUpdateServerIp() {
        server.getInterpretHostServer().preSetRequestResponse(host, HttpException.ERROR_CODE_403, HttpException.ERROR_MSG_SERVICE_LEVEL_DENY, 1);
        getCategory().interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(scheduleService).updateServerIps();
    }


    public static ArgumentMatcher<InterpretHostResponse> match(final InterpretHostResponse interpretHostResponse) {
        return new ArgumentMatcher<InterpretHostResponse>() {
            @Override
            public boolean matches(InterpretHostResponse argument) {
                return interpretHostResponse.equals(argument);
            }
        };
    }

    public static ArgumentMatcher<Throwable> match(final int code, final String message) {
        return new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Throwable argument) {
                if (argument instanceof HttpException) {
                    return ((HttpException) argument).getCode() == code && argument.getMessage().equals(message);
                }
                return false;
            }
        };
    }

}
