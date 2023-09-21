package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.test.server.InterpretHostServer;
import com.alibaba.sdk.android.httpdns.test.server.ResolveHostServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zonglin.nzl
 * @date 2020/12/8
 */
@RunWith(RobolectricTestRunner.class)
public class InterpretHostResultRepoTest {

	private String accountId = RandomValue.randomStringWithFixedLength(8);
	private String host = RandomValue.randomHost();
	private InterpretHostResultRepo repo;
	private String[] ips = RandomValue.randomIpv4s();
	private String[] ipv6s = RandomValue.randomIpv6s();
	private ProbeService ipProbeService = Mockito.mock(ProbeService.class);
	private HttpDnsConfig config;
	private TestExecutorService worker;
	private RecordDBHelper dbHelper;

	private final String REGION_DEFAULT = Constants.REGION_MAINLAND;

	@Before
	public void setUp() {
		worker = new TestExecutorService(new ThreadPoolExecutor(0, 10, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>()));
		config = new HttpDnsConfig(RuntimeEnvironment.application, accountId);
		config.setWorker(worker);
		dbHelper = new RecordDBHelper(config.getContext(), config.getAccountId());
		repo = new InterpretHostResultRepo(config, ipProbeService, dbHelper,
			new InterpretHostCacheGroup());
	}

	/**
	 * 解析结果的 存 读 清除
	 */
	@Test
	public void testSaveUpdateGet() {

		MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v4, null) == null);
		MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v6, null) == null);
		MatcherAssert.assertThat("没有解析过的域名返回空",
			repo.getIps(host, RequestIpType.both, null) == null);

		InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host);

		repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
		UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果",
			repo.getIps(host, RequestIpType.v4, null).getIps(), response.getIps());
		MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v6, null) == null);
		MatcherAssert.assertThat("get方法传入both时，返回空",
			repo.getIps(host, RequestIpType.both, null) == null);

		repo.save(REGION_DEFAULT, host, RequestIpType.v6, null, null, response);
		UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果",
			repo.getIps(host, RequestIpType.v4, null).getIps(), response.getIps());
		UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果",
			repo.getIps(host, RequestIpType.v6, null).getIpv6s(), response.getIpsV6());
		UnitTestUtil.assertIpsEqual("get方法传入both时，返回ipv4 ipv6的结果",
			repo.getIps(host, RequestIpType.both, null).getIps(), response.getIps());
		UnitTestUtil.assertIpsEqual("get方法传入both时，返回ipv4 ipv6的结果",
			repo.getIps(host, RequestIpType.both, null).getIpv6s(), response.getIpsV6());

		repo.update(host, RequestIpType.v4, null, ips);
		UnitTestUtil.assertIpsEqual("更新结果之后再请求，返回更新后的结果",
			repo.getIps(host, RequestIpType.v4, null).getIps(), ips);
		repo.update(host, RequestIpType.v6, null, ipv6s);
		UnitTestUtil.assertIpsEqual("更新结果之后再请求，返回更新后的结果",
			repo.getIps(host, RequestIpType.v6, null).getIpv6s(), ipv6s);

		InterpretHostResponse response1 = InterpretHostServer.randomInterpretHostResponse(host);
		repo.save(REGION_DEFAULT, host, RequestIpType.both, null, null, response1);
		UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果",
			repo.getIps(host, RequestIpType.v4, null).getIps(), response1.getIps());
		UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果",
			repo.getIps(host, RequestIpType.v6, null).getIpv6s(), response1.getIpsV6());
		UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果",
			repo.getIps(host, RequestIpType.both, null).getIps(), response1.getIps());
		UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果",
			repo.getIps(host, RequestIpType.both, null).getIpv6s(), response1.getIpsV6());

		repo.clear();
		MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.v4, null) == null);
		MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.v6, null) == null);
		MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.both, null) == null);
	}

	/**
	 * 解析结果的 存储 更新 本地缓存
	 *
	 * @throws JSONException
	 */
	@Test
	public void testLocalCache() throws JSONException {
		repo.setCachedIPEnabled(true, false);
		try {
			worker.await();
		} catch (InterruptedException e) {
		}
		HashMap<String, InterpretHostResponse> responses = new HashMap<>();
		HashMap<String, RequestIpType> types = new HashMap<>();
		HashMap<String, String> cacheKeys = new HashMap<>();
		HashMap<String, String> extras = new HashMap<>();

		// 存储预解析数据
		for (int k = 0; k < 3; k++) {
			RequestIpType type = RequestIpType.values()[k];
			int preCount = RandomValue.randomInt(10) + 5;
			ArrayList<String> preHosts = new ArrayList<>();
			for (int i = 0; i < preCount; i++) {
				preHosts.add(RandomValue.randomHost());
			}
			ResolveHostResponse resolveHostResponse = ResolveHostServer.randomResolveHostResponse(
				preHosts, type);
			repo.save(REGION_DEFAULT, type, resolveHostResponse);

			for (ResolveHostResponse.HostItem item : resolveHostResponse.getItems()) {
				if (item.getIpType() == RequestIpType.v4) {
					InterpretHostResponse tmp = responses.get(item.getHost());
					responses.put(item.getHost(),
						new InterpretHostResponse(item.getHost(), item.getIps(),
							tmp != null ? tmp.getIpsV6() : null, item.getTtl(), null));
					types.put(item.getHost(), tmp != null ? RequestIpType.both : item.getIpType());
				} else if (item.getIpType() == RequestIpType.v6) {
					InterpretHostResponse tmp = responses.get(item.getHost());
					responses.put(item.getHost(),
						new InterpretHostResponse(item.getHost(), tmp != null ? tmp.getIps() :
                            null,
							item.getIps(), item.getTtl(), null));
					types.put(item.getHost(), tmp != null ? RequestIpType.both : item.getIpType());
				}
				cacheKeys.put(item.getHost(), null);
				extras.put(item.getHost(), null);
			}
		}

		// 存储解析数据
		for (int i = 0; i < 50; i++) {
			String host = RandomValue.randomHost();
			InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host);
			boolean sdns = RandomValue.randomInt(1) == 1;
			String cacheKey = sdns ? RandomValue.randomStringWithMaxLength(10) : null;
			String extra = sdns ? RandomValue.randomJsonMap() : null;
			RequestIpType type = RequestIpType.values()[RandomValue.randomInt(3)];
			repo.save(REGION_DEFAULT, host, type, extra, cacheKey, response);
			responses.put(host, response);
			types.put(host, type);
			cacheKeys.put(host, cacheKey);
			extras.put(host, extra);
		}

		ArrayList<String> hosts = new ArrayList<>(responses.keySet());
		// 更新30个记录
		for (int i = 0; i < 30; i++) {
			String host = hosts.get(RandomValue.randomInt(hosts.size()));
			InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host);
			repo.save(REGION_DEFAULT, host, types.get(host), extras.get(host), cacheKeys.get(host),
				response);
			responses.put(host, response);
		}

		// 更新30个ip记录
		for (int i = 0; i < 30; i++) {
			String host = hosts.get(RandomValue.randomInt(hosts.size()));
			if (types.get(host) != RequestIpType.v6) {
				InterpretHostResponse old = responses.get(host);
				InterpretHostResponse response = new InterpretHostResponse(old.getHostName(),
					RandomValue.randomIpv4s(), old.getIpsV6(), old.getTtl(), old.getExtras());
				repo.update(host, RequestIpType.v4, cacheKeys.get(host), response.getIps());
				responses.put(host, response);
			}
		}

		InterpretHostResultRepo anotherRepo = new InterpretHostResultRepo(config, ipProbeService,
			dbHelper, new InterpretHostCacheGroup());
		anotherRepo.setCachedIPEnabled(true, false);
		try {
			worker.await();
		} catch (InterruptedException e) {
		}

		for (String host : hosts) {
			HTTPDNSResult result = anotherRepo.getIps(host, types.get(host), cacheKeys.get(host));
			if (types.get(host) != RequestIpType.v4) {
				UnitTestUtil.assertIpsEqual("测试repo本地缓存", result.getIpv6s(),
					responses.get(host).getIpsV6());
			}
			if (types.get(host) != RequestIpType.v6) {
				UnitTestUtil.assertIpsEqual("测试repo本地缓存", result.getIps(),
					responses.get(host).getIps());
			}
			if (cacheKeys.get(host) != null) {
				assertExtras("测试repo本地缓存", result.getExtras(), responses.get(host).getExtras());
			}
		}
	}

	/**
	 * 测试定制ttl
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testCacheTtlChanger() throws InterruptedException {

		final int originTtl = 10;
		final int changedTtl = 1;

		CacheTtlChanger changer = Mockito.mock(CacheTtlChanger.class);
		Mockito.when(changer.changeCacheTtl(host, RequestIpType.v4, originTtl)).thenReturn(
			changedTtl);

		InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host,
			originTtl);
		repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
		Thread.sleep(1000);
		MatcherAssert.assertThat("没有设置ttlChanger时，ttl是" + originTtl + ", 1s内不会过期",
			!repo.getIps(host, RequestIpType.v4, null).isExpired());

		repo.setCacheTtlChanger(changer);

		repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
		//        TestLogger.log("start " + System.currentTimeMillis() + " "+repo.getIps(host,
        //        RequestIpType.v4, null).isExpired());
		Thread.sleep(1001);
		//        TestLogger.log("end " + System.currentTimeMillis());
		MatcherAssert.assertThat("设置ttlChanger时，ttl是" + changedTtl + ", 1s会过期",
			repo.getIps(host, RequestIpType.v4, null).isExpired());
		Mockito.verify(changer).changeCacheTtl(host, RequestIpType.v4, originTtl);

		repo.setCacheTtlChanger(null);
		repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
		Thread.sleep(1000);
		MatcherAssert.assertThat("移除ttlchanger后，ttl是" + originTtl + ", 1s不会过期",
			!repo.getIps(host, RequestIpType.v4, null).isExpired());

		final String resolveHost = RandomValue.randomHost();
		ArrayList<String> hostList = new ArrayList<>();
		hostList.add(resolveHost);
		ResolveHostResponse resolveHostResponse = ResolveHostServer.randomResolveHostResponse(
			hostList, RequestIpType.v4, originTtl);

		Mockito.when(changer.changeCacheTtl(resolveHost, RequestIpType.v4, originTtl)).thenReturn(
			changedTtl);
		repo.setCacheTtlChanger(changer);

		repo.save(REGION_DEFAULT, RequestIpType.v4, resolveHostResponse);
		Mockito.verify(changer).changeCacheTtl(resolveHost, RequestIpType.v4, originTtl);
		Thread.sleep(1001);
		MatcherAssert.assertThat("设置ttlChanger时，ttl是" + changedTtl + ", 1s会过期",
			repo.getIps(resolveHost, RequestIpType.v4, null).isExpired());
		Mockito.verify(changer).changeCacheTtl(resolveHost, RequestIpType.v4, originTtl);

	}

	/**
	 * 清理特定host的缓存
	 */
	@Test
	public void testClearTargetHosts() {

		ArrayList<String> hostToBeClear = new ArrayList<>();
		ArrayList<String> hostNotClear = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			String host = RandomValue.randomHost();
			InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host);
			repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
			if (RandomValue.randomInt(2) % 2 == 0) {
				hostToBeClear.add(host);
			} else {
				hostNotClear.add(host);
			}
		}
		for (int i = 0; i < 4; i++) {
			ArrayList<String> tmp = new ArrayList<>();
			for (int j = 0; j < 5; j++) {
				tmp.add(RandomValue.randomHost());
			}
			if (i % 2 == 0) {
				hostToBeClear.addAll(tmp);
			} else {
				hostNotClear.addAll(tmp);
			}
			repo.save(REGION_DEFAULT, RequestIpType.both,
				ResolveHostServer.randomResolveHostResponse(tmp, RequestIpType.both));
		}

		repo.clear(hostToBeClear);

		for (String hostCleared : hostToBeClear) {
			MatcherAssert.assertThat("清除缓存后没有数据",
				repo.getIps(hostCleared, RequestIpType.v4, null) == null);
		}

		for (String hostNotCleared : hostNotClear) {
			MatcherAssert.assertThat("未清除缓存的域名有数据",
				repo.getIps(hostNotCleared, RequestIpType.v4, null) != null);
		}
	}

	/**
	 * 清理内存缓存
	 */
	@Test
	public void testClearMemoryCache() {
		String host = RandomValue.randomHost();
		InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(host);
		ArrayList<String> hosts = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			hosts.add(RandomValue.randomHost());
		}
		// 开启本地缓存
		repo.setCachedIPEnabled(true, false);
		// 触发缓存
		repo.save(REGION_DEFAULT, host, RequestIpType.v4, null, null, response);
		repo.save(REGION_DEFAULT, RequestIpType.v4,
			ResolveHostServer.randomResolveHostResponse(hosts, RequestIpType.v4));
		// 清除内存缓存
		repo.clearMemoryCache();
		MatcherAssert.assertThat("清除缓存后没有数据", repo.getIps(host, RequestIpType.v4, null) == null);

		// 再次设置开启本地缓存，触发读取本地缓存操作
		repo.setCachedIPEnabled(true, false);
		try {
			worker.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		UnitTestUtil.assertIpsEqual("本地缓存不会被清除",
            repo.getIps(host, RequestIpType.v4, null).getIps(),
			response.getIps());
		for (String tmp : hosts) {
			MatcherAssert.assertThat("本地缓存不会被清除",
                repo.getIps(tmp, RequestIpType.v4, null) != null);
		}
	}

	/**
	 * 测试 主站域名的本地缓存，强制开启
	 */
	@Test
	public void testDiskCacheForHostWithFixedIP() {
		ArrayList<String> hostsWithFixedIP = new ArrayList<>();

		String hostWithFixedIP = RandomValue.randomHost();
		InterpretHostResponse responseForHostWithFixedIP
			= InterpretHostServer.randomInterpretHostResponse(hostWithFixedIP);
		hostsWithFixedIP.add(hostWithFixedIP);

		String hostWithoutFixedIP = RandomValue.randomHost();

		ArrayList<String> hosts = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			String tmp = RandomValue.randomHost();
			hosts.add(tmp);
			if (i % 2 == 0) {
				hostsWithFixedIP.add(tmp);
			}
		}

		// 设置主站域名
		repo.setHostListWhichIpFixed(hostsWithFixedIP);

		// 关闭本地缓存，避免干扰
		repo.setCachedIPEnabled(false, false);
		// 触发缓存
		repo.save(REGION_DEFAULT, hostWithFixedIP, RequestIpType.v4, null, null,
			responseForHostWithFixedIP);
		repo.save(REGION_DEFAULT, hostWithoutFixedIP, RequestIpType.v4, null, null,
			InterpretHostServer.randomInterpretHostResponse(hostWithoutFixedIP));
		repo.save(REGION_DEFAULT, RequestIpType.v4,
			ResolveHostServer.randomResolveHostResponse(hosts, RequestIpType.v4));
		String[] updatedIps = RandomValue.randomIpv4s();
		repo.update(hostWithFixedIP, RequestIpType.v4, null, updatedIps);
		// 等待本地缓存完成
		try {
			worker.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 清除内存缓存
		repo.clearMemoryCache();

		// 再次设置开启本地缓存，触发读取本地缓存操作
		repo.setCachedIPEnabled(true, false);
		try {
			worker.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		MatcherAssert.assertThat("非主站域名缓存不存在",
			repo.getIps(hostWithoutFixedIP, RequestIpType.v4, null) == null);
		for (String tmp : hostsWithFixedIP) {
			MatcherAssert.assertThat("主站域名从本地缓存恢复",
				repo.getIps(tmp, RequestIpType.v4, null) != null);
		}
		UnitTestUtil.assertIpsEqual("主站域名从本地缓存恢复",
			repo.getIps(hostWithFixedIP, RequestIpType.v4, null).getIps(), updatedIps);
	}

	/**
	 * 测试 清除非主站域名的内存缓存
	 */
	@Test
	public void testClearMemoryCacheForHostWithoutFixedIP() {
		String hostWithoutFixedIP = RandomValue.randomHost();
		String hostWithFixedIP = RandomValue.randomHost();
		ArrayList<String> hostListWithFixedIP = new ArrayList<>();
		hostListWithFixedIP.add(hostWithFixedIP);

		repo.setHostListWhichIpFixed(hostListWithFixedIP);

		// 触发缓存
		repo.save(REGION_DEFAULT, hostWithFixedIP, RequestIpType.v4, null, null,
			InterpretHostServer.randomInterpretHostResponse(hostWithFixedIP));
		repo.save(REGION_DEFAULT, hostWithoutFixedIP, RequestIpType.v4, null, null,
			InterpretHostServer.randomInterpretHostResponse(hostWithoutFixedIP));
		// 清除内存缓存
		repo.clearMemoryCacheForHostWithoutFixedIP();
		MatcherAssert.assertThat("主站域名没有被清除",
			repo.getIps(hostWithFixedIP, RequestIpType.v4, null) != null);
		MatcherAssert.assertThat("清除缓存后没有数据",
			repo.getIps(hostWithoutFixedIP, RequestIpType.v4, null) == null);
	}

	/**
	 * 获取已缓存解析结果的非主站域名
	 */
	@Test
	public void testGetAllHostWithoutFixedIP() {
		String hostWithoutFixedIP = RandomValue.randomHost();
		String hostWithFixedIP = RandomValue.randomHost();
		ArrayList<String> hostListWithFixedIP = new ArrayList<>();
		hostListWithFixedIP.add(hostWithFixedIP);

		repo.setHostListWhichIpFixed(hostListWithFixedIP);

		// 触发缓存
		repo.save(REGION_DEFAULT, hostWithFixedIP, RequestIpType.v4, null, null,
			InterpretHostServer.randomInterpretHostResponse(hostWithFixedIP));
		repo.save(REGION_DEFAULT, hostWithoutFixedIP, RequestIpType.v4, null, null,
			InterpretHostServer.randomInterpretHostResponse(hostWithoutFixedIP));

		HashMap<String, RequestIpType> result = repo.getAllHostWithoutFixedIP();
		MatcherAssert.assertThat("仅能获取一个非主站域名", result.size() == 1);
		MatcherAssert.assertThat("仅能获取非主站域名", result.get(hostWithoutFixedIP),
			Matchers.is(RequestIpType.v4));
	}

	private void assertExtras(String reason, Map<String, String> extras, String extraStr)
		throws JSONException {
		JSONObject jsonObject = new JSONObject(extraStr);
		if (extras.size() == 0) {
			MatcherAssert.assertThat(reason, extraStr == null);
			return;
		}
		for (String key : extras.keySet()) {
			MatcherAssert.assertThat(reason, extras.get(key).equals(jsonObject.optString(key)));
		}
	}
}
