package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static com.alibaba.sdk.android.httpdns.interpret.NormalCategoryTest.match;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 更新服务IP任务
 *
 * @author zonglin.nzl
 * @date 2020/10/19
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateServerTaskTest {

    private HttpDnsServer server = new HttpDnsServer();
    private RequestCallback<UpdateServerResponse> callback = mock(RequestCallback.class);
    private final String region = "region";
    private HttpDnsConfig httpDnsConfig;
    private TestExecutorService testExecutorService = new TestExecutorService(new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "httpdnstest");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }
    }));
    private ILogger logger;

    @Before
    public void setup() {
        server.start();
        logger = new ILogger() {
            @Override
            public void log(String msg) {
                System.out.println("[Httpdns]" + msg);
            }
        };
        HttpDnsLog.setLogger(logger);
        httpDnsConfig = new HttpDnsConfig(RuntimeEnvironment.application, "100000");
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
    public void sendUpdateServerRequestAndGetNewServers() {
        UpdateServerResponse updateServerResponse = new UpdateServerResponse(RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), RandomValue.randomPorts(), RandomValue.randomPorts());
        server.getServerIpsServer().preSetRequestResponse(region, updateServerResponse, -1);
        UpdateServerTask.updateServer(httpDnsConfig, region, callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback).onSuccess(updateServerResponse);
        verify(callback, never()).onFail(any(Throwable.class));
    }

    @Test
    public void sendUpdateServerRequestAndServerNotAvailable() {
        server.getServerIpsServer().preSetRequestResponse(region, 403, "whatever", -1);
        UpdateServerTask.updateServer(httpDnsConfig, region, callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback, never()).onSuccess(any(UpdateServerResponse.class));
        verify(callback).onFail(argThat(match(403, "whatever")));
    }

    @Test
    public void sendUpdateServerRequestAndServerNotReachable() {
        server.getServerIpsServer().preSetRequestTimeout(region, -1);
        UpdateServerTask.updateServer(httpDnsConfig, region, callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(callback, never()).onSuccess(any(UpdateServerResponse.class));
        verify(callback).onFail(any(Throwable.class));
    }

}
