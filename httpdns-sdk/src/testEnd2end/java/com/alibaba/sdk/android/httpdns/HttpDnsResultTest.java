package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

/**
 * @author zonglin.nzl
 * @date 11/8/21
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class HttpDnsResultTest {

    @Test
    public void updateContentWithHostRecord() {

        final String host = RandomValue.randomHost();
        HTTPDNSResult httpdnsResult = new HTTPDNSResult(host);

        HostRecord record = HostRecord.create(Constants.REGION_MAINLAND, host, RequestIpType.v4, null, null, RandomValue.randomIpv4s(), 60);
        httpdnsResult.update(record);

        MatcherAssert.assertThat("使用HostRecord更新result", httpdnsResult.getIps(), Matchers.is(Matchers.equalTo(record.getIps())));

        HostRecord record1 = HostRecord.create(Constants.REGION_MAINLAND, host, RequestIpType.v6, null, null, RandomValue.randomIpv6s(), 60);
        httpdnsResult.update(record1);

        MatcherAssert.assertThat("更新ipv6 不会影响v4的结果", httpdnsResult.getIps(), Matchers.is(Matchers.equalTo(record.getIps())));
        MatcherAssert.assertThat("更新ipv6", httpdnsResult.getIpv6s(), Matchers.is(Matchers.equalTo(record1.getIps())));

        record.setQueryTime(0);
        httpdnsResult.update(record);
        MatcherAssert.assertThat("更新为过期", httpdnsResult.isExpired());

        record1.setExtra("{\"name\":\"BeJson\",\"url\":\"http://www.bejson.com\",\"page\":88,\"isNonProfit\":true}");
        httpdnsResult.update(record1);
        MatcherAssert.assertThat("更新为不过期", !httpdnsResult.isExpired());
        MatcherAssert.assertThat("更新extra", httpdnsResult.getExtras().get("name"), Matchers.is(Matchers.equalTo("BeJson")));

    }

    @Test
    public void updateContentWithHostRecords() {
        final String host = RandomValue.randomHost();
        HostRecord record = HostRecord.create(Constants.REGION_MAINLAND, host, RequestIpType.v4, null, null, RandomValue.randomIpv4s(), 60);
        record.setQueryTime(0);
        HostRecord record1 = HostRecord.create(Constants.REGION_MAINLAND, host, RequestIpType.v6, "{\"name\":\"BeJson\",\"url\":\"http://www.bejson.com\",\"page\":88,\"isNonProfit\":true}", null, RandomValue.randomIpv6s(), 60);
        record1.setFromDB(true);

        HTTPDNSResult result = new HTTPDNSResult(host);
        ArrayList<HostRecord> records = new ArrayList<>();
        records.add(record);
        records.add(record1);
        result.update(records);


        MatcherAssert.assertThat("更新ip", result.getIps(), Matchers.is(Matchers.equalTo(record.getIps())));
        MatcherAssert.assertThat("更新ipv6", result.getIpv6s(), Matchers.is(Matchers.equalTo(record1.getIps())));
        MatcherAssert.assertThat("更新过期时间", result.isExpired());
        MatcherAssert.assertThat("更新Extra", result.getExtras().get("name"), Matchers.is(Matchers.equalTo("BeJson")));
        MatcherAssert.assertThat("更新isFromDB", result.isFromDB());

    }
}
