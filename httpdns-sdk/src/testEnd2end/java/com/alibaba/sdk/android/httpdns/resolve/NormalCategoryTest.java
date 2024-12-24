package com.alibaba.sdk.android.httpdns.resolve;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * 域名解析任务的测试
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
@RunWith(RobolectricTestRunner.class)
public class NormalCategoryTest extends BaseCategoryTest {

    private NormalResolveCategory category = new NormalResolveCategory(scheduleService, statusControl);

    @Override
    public NormalResolveCategory getCategory() {
        return category;
    }

    @Test
    public void retryOnceWhenFail() {
        server.getResolveHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        category.resolve(httpDnsConfig, ResolveHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean requestTwice = server.getResolveHostServer().hasRequestForArgWithResult(host, 403, "whatever", 2, true);
        MatcherAssert.assertThat("normal category retry request when failed", requestTwice);
    }
}
