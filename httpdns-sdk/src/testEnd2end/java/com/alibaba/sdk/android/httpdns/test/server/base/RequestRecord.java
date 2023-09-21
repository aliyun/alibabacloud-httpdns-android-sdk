package com.alibaba.sdk.android.httpdns.test.server.base;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务请求记录
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class RequestRecord {
    private RecordedRequest recordedRequest;
    private MockResponse mockResponse;

    public RequestRecord(RecordedRequest recordedRequest, MockResponse mockResponse) {
        this.recordedRequest = recordedRequest;
        this.mockResponse = mockResponse;
    }

    public RecordedRequest getRecordedRequest() {
        return recordedRequest;
    }

    public MockResponse getMockResponse() {
        return mockResponse;
    }
}
