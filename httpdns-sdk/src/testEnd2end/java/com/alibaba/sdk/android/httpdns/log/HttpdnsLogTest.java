package com.alibaba.sdk.android.httpdns.log;

import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author zonglin.nzl
 * @date 2020/10/16
 */
@RunWith(RobolectricTestRunner.class)
public class HttpdnsLogTest {
    private ILogger mockLogger = mock(ILogger.class);

    @Before
    public void setUp() {
        HttpDnsLog.setLogger(mockLogger);
    }

    @After
    public void tearDown() {
        HttpDnsLog.removeLogger(mockLogger);
    }

    @Test
    public void translateStringArrayToStrForLog() {
        String[] ips = RandomValue.randomIpv4s();
        String result = HttpDnsLog.wrap(ips);

        for (int i = 0; i < ips.length; i++) {
            assertThat(result, containsString(ips[i]));
        }
    }

    @Test
    public void receiveLogWithLoggerForLogE() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);

        String errLog = "Error msg";
        HttpDnsLog.e(errLog);
        verify(mockLogger).log(logArgument.capture());
        assertThat(logArgument.getValue(), containsString(errLog));

        HttpDnsLog.e(errLog, new NullPointerException("NPE error test"));
        verify(mockLogger, atLeast(2)).log(anyString());
    }


    @Test
    public void receiveLogWithLoggerForLogW() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);

        String warnLog = "Warn msg";
        HttpDnsLog.w(warnLog);
        verify(mockLogger).log(logArgument.capture());
        assertThat(logArgument.getValue(), containsString(warnLog));

        HttpDnsLog.w(warnLog, new NullPointerException("NPE warn test"));
        verify(mockLogger, atLeast(2)).log(anyString());

    }

    @Test
    public void receiveLogWithLoggerForLogI() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);

        String infoLog = "Info msg";
        HttpDnsLog.i(infoLog);
        verify(mockLogger).log(logArgument.capture());
        assertThat(logArgument.getValue(), containsString(infoLog));
    }


    @Test
    public void receiveLogWithLoggerForLogD() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);

        String debugLog = "Debug msg";
        HttpDnsLog.d(debugLog);
        verify(mockLogger).log(logArgument.capture());
        assertThat(logArgument.getValue(), containsString(debugLog));
    }

    @Test
    public void enableLogMeansPrintLogInLogcat() {
        HttpDnsLog.enable(true);
        String errLog = "Error msg";
        HttpDnsLog.e(errLog);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, errLog, null);

        NullPointerException npe = new NullPointerException("NPE error test");
        HttpDnsLog.e(errLog, npe);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, errLog, npe);

        String warnLog = "Warn msg";
        HttpDnsLog.w(warnLog);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, warnLog, null);

        NullPointerException npeWarn = new NullPointerException("NPE warn test");
        HttpDnsLog.w(warnLog, npeWarn);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, warnLog, npeWarn);

        String infoLog = "Info msg";
        HttpDnsLog.i(infoLog);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, infoLog, null);

        String debugLog = "Debug msg";
        HttpDnsLog.d(debugLog);
        verifyLogcatReceiveLog(HttpDnsLog.TAG, debugLog, null);

        HttpDnsLog.enable(false);
        HttpDnsLog.e(errLog);
        HttpDnsLog.e(errLog, new NullPointerException("NPE error test"));
        HttpDnsLog.w(warnLog);
        HttpDnsLog.w(warnLog, new NullPointerException("NPE warn test"));
        HttpDnsLog.i(infoLog);
        HttpDnsLog.d(debugLog);
        List<ShadowLog.LogItem> emptyList = ShadowLog.getLogsForTag("TestTag1");
        assertThat(emptyList, anyOf(nullValue(), Matchers.hasSize(0)));
    }

    private void verifyLogcatReceiveLog(String tag, String msg, Throwable throwable) {
        List<ShadowLog.LogItem> list = ShadowLog.getLogsForTag(tag);
        assertThat(list.size(), is(equalTo(1)));
        assertThat(list.get(0).msg, is(equalTo(msg)));
        if (throwable != null) {
            assertThat(list.get(0).throwable, is(equalTo(throwable)));
        }
        ShadowLog.clear();
    }
}
