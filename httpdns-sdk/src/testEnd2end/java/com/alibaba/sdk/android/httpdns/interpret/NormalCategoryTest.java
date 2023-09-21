package com.alibaba.sdk.android.httpdns.interpret;

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

    private NormalCategory category = new NormalCategory(scheduleService, statusControl);

    @Override
    public NormalCategory getCategory() {
        return category;
    }

    @Test
    public void retryOnceWhenFail() {
        server.getInterpretHostServer().preSetRequestResponse(host, 403, "whatever", -1);
        category.interpret(httpDnsConfig, InterpretHostHelper.getIpv4Config(httpDnsConfig, host), callback);
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean requestTwice = server.getInterpretHostServer().hasRequestForArgWithResult(host, 403, "whatever", 2, true);
        MatcherAssert.assertThat("normal category retry request when failed", requestTwice);
    }
}
