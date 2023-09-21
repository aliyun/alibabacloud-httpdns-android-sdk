package com.alibaba.sdk.android.httpdns.interpret;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class SniffCategoryTest extends BaseCategoryTest {
    private SniffCategory category = new SniffCategory(scheduleService, statusControl);

    @Override
    protected InterpretHostCategory getCategory() {
        return category;
    }

    @Test
    public void noRetryWhenFail() {
        server.getInterpretHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        category.interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean requestOnce = server.getInterpretHostServer().hasRequestForArgWithResult(host, 403, "whatever", 1, true);
        MatcherAssert.assertThat("normal category retry request when failed", requestOnce);
    }

    @Test
    public void requestHasInterval() {
        category.setInterval(1000);
        category.interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        category.interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MatcherAssert.assertThat("sniff will not request in interval time", server.getInterpretHostServer().hasRequestForArg(host, 1, false));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        category.interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MatcherAssert.assertThat("sniff will request after interval time", server.getInterpretHostServer().hasRequestForArg(host, 2, false));
    }

}
