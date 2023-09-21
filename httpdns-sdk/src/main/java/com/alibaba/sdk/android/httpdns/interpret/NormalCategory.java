package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;

/**
 * 域名解析的一般策略
 */
public class NormalCategory implements InterpretHostCategory {

    private final StatusControl mStatusControl;
    private final ScheduleService mScheduleService;

    public NormalCategory(ScheduleService scheduleService, StatusControl statusControl) {
        this.mScheduleService = scheduleService;
        this.mStatusControl = statusControl;
    }

    @Override
    public void interpret(HttpDnsConfig config, HttpRequestConfig requestConfig, RequestCallback<InterpretHostResponse> callback) {
        HttpRequest<InterpretHostResponse> request = new HttpRequest<>(requestConfig,
            new InterpretHostResponseTranslator());
        request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 切换服务IP，更新服务IP
        request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config, mScheduleService,
            mStatusControl));
        // 重试一次
        request = new RetryHttpRequest<>(request, 1);
        try {
            config.getWorker().execute(new HttpRequestTask<>(request, callback));
        } catch (Throwable e) {
            callback.onFail(e);
        }
    }

}
