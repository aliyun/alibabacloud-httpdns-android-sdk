package com.alibaba.sdk.android.httpdns.resolve;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.ranking.IPRankingService;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 在性能测试时，我们发现
 * 1. 性能问题出在我们频繁的创建对象 HTTPDNSResult
 * 2. 性能问题出在我们频繁的创建对象 缓存的key 是动态生成的
 * <p>
 * 所以我们期望，缓存如果命中，就不应该产生任何对象创建
 *
 * @author zonglin.nzl
 * @date 11/8/21
 */
@RunWith(RobolectricTestRunner.class)
public class ResolveHostResultRepoTest2 {

    private ResolveHostResultRepo repo;
    private RecordDBHelper dbHelper;
    private TestExecutorService worker;

    private final String REGION_DEFAULT = Constants.REGION_MAINLAND;

    @Before
    public void setup() {
        worker = new TestExecutorService(new ThreadPoolExecutor(0, 10, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()));
        dbHelper = Mockito.mock(RecordDBHelper.class);
        HttpDnsConfig config = new HttpDnsConfig(RuntimeEnvironment.application, "aaa");
        config.setWorker(worker);
        repo = new ResolveHostResultRepo(config, Mockito.mock(IPRankingService.class), dbHelper, new ResolveHostCacheGroup());
    }

    /**
     * 命中缓存 使用相同的对象
     */
    @Test
    public void cacheHitWillReturnSameObject() {

        final String host = RandomValue.randomHost();

        repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, new ResolveHostResponse(host, new String[]{"a.b.c"}, null, 60, null));

        HTTPDNSResult result = repo.getIps(host, RequestIpType.v4, null);
        HTTPDNSResult result1 = repo.getIps(host, RequestIpType.v4, null);

        MatcherAssert.assertThat("缓存命中，应该返回相同的对象", result == result1);

    }

    /**
     * 更新时，更新对象的属性，不修改对象
     */
    @Test
    public void saveWillUpdateCacheContentNotInstance() {
        final String host = RandomValue.randomHost();

        String ip1 = RandomValue.randomIpv4();
        repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, new ResolveHostResponse(host, new String[]{ip1}, null, 60, null));

        HTTPDNSResult result = repo.getIps(host, RequestIpType.v4, null);

        MatcherAssert.assertThat("开始时，缓存是 " + ip1, result.getIps()[0], Matchers.is(Matchers.equalTo(ip1)));

        String ip2 = RandomValue.randomIpv4();
        repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, new ResolveHostResponse(host, new String[]{ip2}, null, 60, null));

        HTTPDNSResult result1 = repo.getIps(host, RequestIpType.v4, null);

        MatcherAssert.assertThat("缓存更新", result1.getIps()[0], Matchers.is(Matchers.equalTo(ip2)));
        MatcherAssert.assertThat("缓存更新时，更新的是缓存内容，不重新创建缓存对象", result == result1);
    }

    /**
     * 超过ttl 会过期
     * @throws InterruptedException
     */
    @Test
    public void httpDnsResultExpiredAfterTtl() throws InterruptedException {
        final String host = RandomValue.randomHost();
        String ip1 = RandomValue.randomIpv4();
        repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, new ResolveHostResponse(host, new String[]{ip1}, null, 1, null));

        HTTPDNSResult result = repo.getIps(host, RequestIpType.v4, null);

        MatcherAssert.assertThat("在ttl时间内，不过期", result.isExpired(), Matchers.is(false));

        Thread.sleep(1100);

        result = repo.getIps(host, RequestIpType.v4, null);
        MatcherAssert.assertThat("超过ttl时间过期", result.isExpired(), Matchers.is(true));
    }
}
