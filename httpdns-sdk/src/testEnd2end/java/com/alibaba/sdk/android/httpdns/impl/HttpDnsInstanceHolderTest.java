package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import android.util.Log;

/**
 * @author zonglin.nzl
 * @date 2020/12/4
 */
@RunWith(RobolectricTestRunner.class)
public class HttpDnsInstanceHolderTest {

	private HttpDnsCreator creator = Mockito.mock(HttpDnsCreator.class);
	private HttpDnsInstanceHolder holder = new HttpDnsInstanceHolder(creator);
	private String account1 = RandomValue.randomStringWithFixedLength(10);
	private String account2 = RandomValue.randomStringWithFixedLength(10);
	private HttpDnsServiceImpl service1 = Mockito.mock(HttpDnsServiceImpl.class);
	private HttpDnsServiceImpl service2 = Mockito.mock(HttpDnsServiceImpl.class);

	@Before
	public void setUp() {
		Mockito.when(
			creator.create(Mockito.eq(RuntimeEnvironment.application), Mockito.eq(account1),
				Mockito.anyString())).thenReturn(service1);
		Mockito.when(
			creator.create(Mockito.eq(RuntimeEnvironment.application), Mockito.eq(account1),
				(String)Mockito.isNull())).thenReturn(service1);
		Mockito.when(
			creator.create(Mockito.eq(RuntimeEnvironment.application), Mockito.eq(account2),
				Mockito.anyString())).thenReturn(service2);
		Mockito.when(
			creator.create(Mockito.eq(RuntimeEnvironment.application), Mockito.eq(account2),
				(String)Mockito.isNull())).thenReturn(service2);
	}

	@Test
	public void sameAccountSameInstance() {
		MatcherAssert.assertThat("create instance if not exist",
			holder.get(RuntimeEnvironment.application, account1,
				RandomValue.randomStringWithMaxLength(40)) != null);
		MatcherAssert.assertThat("same account use same instance",
			holder.get(RuntimeEnvironment.application, account1, null) == holder.get(
				RuntimeEnvironment.application, account1,
				RandomValue.randomStringWithMaxLength(40)));
	}

	@Test
	public void differentAccountGetDifferentInstance() {
		MatcherAssert.assertThat("create instance if not exist",
			holder.get(RuntimeEnvironment.application, account2, null) != null);
		MatcherAssert.assertThat("same account use same instance",
			holder.get(RuntimeEnvironment.application, account2, null) != holder.get(
				RuntimeEnvironment.application, account1,
				RandomValue.randomStringWithMaxLength(40)));
	}

	@Test
	public void secretWillBeAdded() {
		holder.get(RuntimeEnvironment.application, account1, null);
		String secretKey = RandomValue.randomStringWithMaxLength(40);
		holder.get(RuntimeEnvironment.application, account1, secretKey);
		holder.get(RuntimeEnvironment.application, account2, null);
		Mockito.verify(service1).setSecret(secretKey);
		Mockito.verify(service2, Mockito.never()).setSecret(Mockito.anyString());
	}
}
