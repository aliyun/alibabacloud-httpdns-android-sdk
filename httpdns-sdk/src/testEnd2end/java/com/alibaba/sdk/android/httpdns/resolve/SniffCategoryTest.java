package com.alibaba.sdk.android.httpdns.resolve;

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
    private SniffResolveCategory category = new SniffResolveCategory(scheduleService, statusControl);

    @Override
    protected ResolveHostCategory getCategory() {
        return category;
    }

    @Test
    public void noRetryWhenFail() {
        server.getResolveHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        category.resolve(httpDnsConfig, ResolveHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean requestOnce = server.getResolveHostServer().hasRequestForArgWithResult(host, 403, "whatever", 1, true);
        MatcherAssert.assertThat("normal category retry request when failed", requestOnce);
    }

    @Test
    public void requestHasInterval() {
        category.setInterval(1000);
        category.resolve(httpDnsConfig, ResolveHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        category.resolve(httpDnsConfig, ResolveHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MatcherAssert.assertThat("sniff will not request in interval time", server.getResolveHostServer().hasRequestForArg(host, 1, false));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        category.resolve(httpDnsConfig, ResolveHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MatcherAssert.assertThat("sniff will request after interval time", server.getResolveHostServer().hasRequestForArg(host, 2, false));
    }

}
