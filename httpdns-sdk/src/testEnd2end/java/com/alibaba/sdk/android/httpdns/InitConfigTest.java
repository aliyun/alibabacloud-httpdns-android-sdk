package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class InitConfigTest {

    @Before
    public void setup() {
        InitConfig.removeConfig(null);
    }

    @Test
    public void configAndGetLater() {
        String accountId = RandomValue.randomStringWithFixedLength(10);
        InitConfig config = new InitConfig.Builder().buildFor(accountId);

        MatcherAssert.assertThat("创建的config可以通过get方法获取", InitConfig.getInitConfig(accountId), Matchers.is(config));
    }

    @Test
    public void configDefaultValues() {
        String accountId = RandomValue.randomStringWithFixedLength(10);
        new InitConfig.Builder().buildFor(accountId);
        InitConfig config = InitConfig.getInitConfig(accountId);

        MatcherAssert.assertThat("默认允许过期IP", config.isEnableExpiredIp(), Matchers.is(true));
        MatcherAssert.assertThat("默认不本地缓存IP", config.isEnableCacheIp(), Matchers.is(false));
        MatcherAssert.assertThat("默认不开启https", config.isEnableHttps(), Matchers.is(false));
        MatcherAssert.assertThat("默认region正确", config.getRegion(), Matchers.is(InitConfig.NOT_SET));
        MatcherAssert.assertThat("默认超时时间是15s", config.getTimeout(), Matchers.is(15 * 1000));
        MatcherAssert.assertThat("默认不测速", config.getIPRankingList(), Matchers.nullValue());
        MatcherAssert.assertThat("默认不修改缓存ttl配置", config.getCacheTtlChanger(), Matchers.nullValue());
    }

    @Test
    public void getConfigValue() {
        String accountId = RandomValue.randomStringWithFixedLength(10);
        CacheTtlChanger ttlChanger = Mockito.mock(CacheTtlChanger.class);
        new InitConfig.Builder()
                .setEnableExpiredIp(false)
                .setEnableCacheIp(true)
                .setEnableHttps(true)
                .setRegion(Constants.REGION_HK)
                .setTimeout(5 * 1000)
                .setIPRankingList(Arrays.asList(new IPRankingBean("aa", 43)))
                .configCacheTtlChanger(ttlChanger)
                .buildFor(accountId);
        InitConfig config = InitConfig.getInitConfig(accountId);

        MatcherAssert.assertThat("不允许过期IP", config.isEnableExpiredIp(), Matchers.is(false));
        MatcherAssert.assertThat("本地缓存IP", config.isEnableCacheIp(), Matchers.is(true));
        MatcherAssert.assertThat("开启https", config.isEnableHttps(), Matchers.is(true));
        MatcherAssert.assertThat("region是HK", config.getRegion(), Matchers.is(Constants.REGION_HK));
        MatcherAssert.assertThat("超时时间是5s", config.getTimeout(), Matchers.is(5 * 1000));
        MatcherAssert.assertThat("测速", config.getIPRankingList().get(0).getHostName(), Matchers.is(Matchers.equalTo("aa")));
        MatcherAssert.assertThat("测速", config.getIPRankingList().get(0).getPort(), Matchers.is(43));
        MatcherAssert.assertThat("配置的有ttlChanger", config.getCacheTtlChanger(), Matchers.is(ttlChanger));
    }
}
