package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class HostFilterTest {
    private DegradationFilter filter = Mockito.mock(DegradationFilter.class);
    private HostFilter hostFilter = new HostFilter();

    @Test
    public void testFilter() {
        String blockHost = RandomValue.randomHost();
        String normalHost = RandomValue.randomHost();
        Mockito.when(filter.shouldDegradeHttpDNS(blockHost)).thenReturn(true);
        Mockito.when(filter.shouldDegradeHttpDNS(normalHost)).thenReturn(false);

        hostFilter.setFilter(filter);

        MatcherAssert.assertThat("过滤的域名", hostFilter.isFiltered(blockHost));
        MatcherAssert.assertThat("不过滤的域名", hostFilter.isFiltered(blockHost));
    }
}
