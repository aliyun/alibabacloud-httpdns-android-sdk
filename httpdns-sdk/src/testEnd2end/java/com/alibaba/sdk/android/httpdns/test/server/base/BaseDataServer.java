package com.alibaba.sdk.android.httpdns.test.server.base;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

/**
 * 数据类型服务的基础逻辑
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public abstract class BaseDataServer<ARG, DATA> implements ServerApi<ARG, DATA>, SdkBusinessServer {

    private HashMap<ARG, ArrayList<HttpResponse>> preSetResponse = new HashMap<>();
    protected ArrayList<RequestRecord> records = new ArrayList<>();

    @Override
    public void preSetRequestTimeout(ARG arg, int count) {
        ArrayList<HttpResponse> responses = preSetResponse.get(arg);
        if (responses == null) {
            responses = new ArrayList<>();
            preSetResponse.put(arg, responses);
        }
        responses.add(new HttpResponse(count, true));
    }

    @Override
    public void preSetRequestResponse(ARG arg, DATA data, int count) {
        preSetRequestResponse(arg, new HttpResponse(200, convert(data), count));
    }

    @Override
    public void preSetRequestResponse(ARG arg, int httpCode, String body, int count) {
        preSetRequestResponse(arg, new HttpResponse(httpCode, body, count));
    }

    private void preSetRequestResponse(ARG arg, HttpResponse response) {
        ArrayList<HttpResponse> responses = preSetResponse.get(arg);
        if (responses == null) {
            responses = new ArrayList<>();
            preSetResponse.put(arg, responses);
        }
        responses.add(response);
    }

    @Override
    public boolean hasRequestForArg(ARG arg, int count, boolean removeRecord) {
        ArrayList<RequestRecord> requestRecords = findRequestRecords(arg);
        if (removeRecord && requestRecords.size() != 0) {
            synchronized (records) {
                records.removeAll(requestRecords);
            }
        }
        if (count >= 0) {
            return requestRecords.size() == count;
        } else {
            return requestRecords.size() > 0;
        }
    }

    @Override
    public boolean hasRequestForArgWithParams(ARG arg, List<String> params, int count, boolean removeRecord) {
        ArrayList<RequestRecord> requestRecords = findRequestRecords(arg);
        filterWithParams(requestRecords, params);
        if (removeRecord && requestRecords.size() != 0) {
            synchronized (records) {
                records.removeAll(requestRecords);
            }
        }
        if (count >= 0) {
            return requestRecords.size() == count;
        } else {
            return requestRecords.size() > 0;
        }
    }

    private void filterWithParams(ArrayList<RequestRecord> requestRecords, List<String> params) {
        ArrayList<RequestRecord> filtered = new ArrayList<>();
        for (RequestRecord record : requestRecords) {
            for (String param : params) {
                if (record.getRecordedRequest().getRequestUrl().queryParameter(param) == null) {
                    filtered.add(record);
                    break;
                }
            }
        }
        requestRecords.removeAll(filtered);
    }

    @Override
    public boolean hasRequestForArgTimeout(ARG arg, int count, boolean removeRecord) {
        ArrayList<RequestRecord> requestRecords = findRequestRecords(arg);
        ArrayList<RequestRecord> targetRecords = new ArrayList<>();
        for (RequestRecord record : requestRecords) {
            if (record.getMockResponse().getSocketPolicy().equals(SocketPolicy.NO_RESPONSE)) {
                targetRecords.add(record);
            }
        }
        if (removeRecord && targetRecords.size() != 0) {
            synchronized (records) {
                records.removeAll(targetRecords);
            }
        }
        if (count >= 0) {
            return targetRecords.size() == count;
        } else {
            return targetRecords.size() > 0;
        }
    }

    @Override
    public boolean hasRequestForArgWithResult(ARG arg, DATA data, int count, boolean removeRecord) {
        String body = convert(data);
        return hasRequestForArgWithResult(arg, 200, body, count, removeRecord);
    }

    @Override
    public boolean hasRequestForArgWithResult(ARG arg, int httpCode, String body, int count, boolean removeRecord) {
        ArrayList<RequestRecord> requestRecords = findRequestRecords(arg);
        ArrayList<RequestRecord> targetRecords = new ArrayList<>();
        for (RequestRecord record : requestRecords) {
            if (record.getMockResponse().getStatus().contains("" + httpCode)
                    && record.getMockResponse().getBody().readString(Charset.forName("UTF-8")).equals(body)) {
                targetRecords.add(record);
            }
        }
        if (removeRecord && targetRecords.size() != 0) {
            synchronized (records) {
                records.removeAll(targetRecords);
            }
        }
        if (count >= 0) {
            return targetRecords.size() == count;
        } else {
            return targetRecords.size() > 0;
        }
    }

    @Override
    public List<DATA> getResponse(ARG arg, int count, boolean removeRecord) {
        ArrayList<RequestRecord> records = findRequestRecords(arg);
        if (records == null) {
            return null;
        }
        ArrayList<RequestRecord> targetRecords = new ArrayList<>();
        for (RequestRecord record : records) {
            if (record.getMockResponse().getStatus().contains("HTTP/1.1 200")) {
                targetRecords.add(record);
            }
        }
        if (targetRecords.size() > count) {
            targetRecords.subList(0, count);
        }
        ArrayList<DATA> datas = new ArrayList<>();
        for (RequestRecord record : targetRecords) {
            datas.add(convert(record.getMockResponse().getBody().readString(Charset.forName("UTF-8"))));
        }
        if (removeRecord) {
            synchronized (this.records) {
                this.records.removeAll(targetRecords);
            }
        }
        return datas;
    }

    @Override
    public void cleanRecord() {
        synchronized (this.records) {
            this.records.clear();
        }
    }

    @Override
    public MockResponse handle(RecordedRequest request) {
        ARG arg = getRequestArg(request);
        HttpResponse httpResponse = getPreSetResponse(arg);
        MockResponse mockResponse = new MockResponse();
        if (httpResponse != null && httpResponse.timeout) {
            mockResponse.setSocketPolicy(SocketPolicy.NO_RESPONSE);
        } else if (httpResponse == null) {
            mockResponse.setResponseCode(200).setBody(convert(randomData(arg)));
        } else {
            mockResponse.setResponseCode(httpResponse.code).setBody(httpResponse.body);
        }
        synchronized (records) {
            records.add(new RequestRecord(request, mockResponse));
        }
        return mockResponse;
    }

    /**
     * 获取预置数据
     *
     * @param arg
     * @return
     */
    protected HttpResponse getPreSetResponse(ARG arg) {
        ArrayList<HttpResponse> responses = preSetResponse.get(arg);
        if (responses == null || responses.size() == 0) {
            return null;
        }
        HttpResponse response = responses.get(0);
        if (response.count > 0) {
            response.count--;
        }
        if (response.count == 0) {
            responses.remove(0);
        }
        return response;
    }

    /**
     * 获取请求参数
     *
     * @param recordedRequest
     * @return
     */
    public abstract ARG getRequestArg(RecordedRequest recordedRequest);

    /**
     * 将数据转化为响应
     *
     * @param data
     * @return
     */
    public abstract String convert(DATA data);

    /**
     * 将body转化为数据
     *
     * @param body
     * @return
     */
    public abstract DATA convert(String body);

    /**
     * 生成随机数据，用于没有预置数据时
     *
     * @param arg
     * @return
     */
    public abstract DATA randomData(ARG arg);

    private ArrayList<RequestRecord> findRequestRecords(ARG arg) {
        ArrayList<RequestRecord> requestRecords = new ArrayList<>();
        synchronized (records) {
            for (RequestRecord record : records) {
                ARG requestArg = getRequestArg(record.getRecordedRequest());
                if (arg.equals(requestArg)) {
                    requestRecords.add(record);
                }
            }
        }
        return requestRecords;
    }

    public static class HttpResponse {
        public int code;
        public String body;
        public int count = -1;
        public boolean timeout = false;

        public HttpResponse(int code, String body, int count) {
            this.code = code;
            this.body = body;
            this.count = count;
        }

        public HttpResponse(int count, boolean timeout) {
            this.count = count;
            this.timeout = timeout;
        }
    }
}
