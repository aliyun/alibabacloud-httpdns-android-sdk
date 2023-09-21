package com.alibaba.sdk.android.httpdns.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author zonglin.nzl
 * @date 2020/10/16
 */
@RunWith(RobolectricTestRunner.class)
public class HttpExceptionTest {

    @Test
    public void parseCodeInHttpMsg() {
        HttpException exception = HttpException.create(123, "{\"code\":\"hello\"}");

        assertThat(exception.getCode(), is(equalTo(123)));
        assertThat(exception.getMessage(), is(equalTo("hello")));
    }

    @Test
    public void doNotParseInvalidMsg() {
        HttpException exception = HttpException.create(123, "hello");

        assertThat(exception.getCode(), is(equalTo(123)));
        assertThat(exception.getMessage(), is(equalTo("hello")));
    }
}
