package com.alibaba.sdk.android.httpdns.observable;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.config.SpCacheItem;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class ObservableConfig implements SpCacheItem {
    //默认关闭
    public boolean enable = false;
    public double sampleRatio = 1.0;
    public String endpoint = "";
    public int batchReportMaxSize = 50;
    public int batchReportIntervalTime = 60;
    public int maxReportsPerMinute = 4;
    public JSONObject raw = null;

    public boolean updateConfig(ObservableConfig config) {
        if (config == null) {
            //调度接口没有下发配置，关闭可观测
            if (enable) {
                enable = false;
                raw = null;
                return true;
            }
            return false;
        }

        if (isSameConfig(config)) {
            return false;
        }

        enable = config.enable;
        sampleRatio = config.sampleRatio;
        endpoint = config.endpoint;
        batchReportMaxSize = config.batchReportMaxSize;
        batchReportIntervalTime = config.batchReportIntervalTime;
        maxReportsPerMinute = config.maxReportsPerMinute;
        raw = config.raw;
        return true;
    }

    private boolean isSameConfig(ObservableConfig config) {
        return enable == config.enable
                && sampleRatio == config.sampleRatio
                && TextUtils.equals(endpoint, config.endpoint)
                && batchReportMaxSize == config.batchReportMaxSize
                && batchReportIntervalTime == config.batchReportIntervalTime
                && maxReportsPerMinute == config.maxReportsPerMinute;
    }

    public static ObservableConfig fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        ObservableConfig observableConfig = new ObservableConfig();
        if (json.has("enable")) {
            observableConfig.enable = json.optBoolean("enable");
        }
        if (json.has("sample_ratio")) {
            observableConfig.sampleRatio = json.optDouble("sample_ratio");
        }
        String endpoint = json.optString("endpoint");
        if (!TextUtils.isEmpty(endpoint)) {
            observableConfig.endpoint = endpoint;
        }
        observableConfig.batchReportMaxSize = json.optInt("batch_report_max_size", 50);
        if (observableConfig.batchReportMaxSize <= 0) {
            observableConfig.batchReportMaxSize = 50;
        }
        observableConfig.batchReportIntervalTime = json.optInt("batch_report_interval_time", 60);
        if (observableConfig.batchReportIntervalTime <= 0) {
            observableConfig.batchReportIntervalTime = 60;
        }
        observableConfig.maxReportsPerMinute = json.optInt("max_reports_per_minute", 4);
        if (observableConfig.maxReportsPerMinute <= 0) {
            observableConfig.maxReportsPerMinute = 4;
        }
        observableConfig.raw = json;

        return observableConfig;
    }

    @Override
    public void restoreFromCache(SharedPreferences sp) {
        String cachedObservableConfig = sp.getString(Constants.CONFIG_OBSERVABLE_CONFIG, null);
        if (!TextUtils.isEmpty(cachedObservableConfig)) {
            try {
                JSONObject json = new JSONObject(cachedObservableConfig);
                ObservableConfig cachedConfig = fromJson(json);
                enable = cachedConfig.enable;
                sampleRatio = cachedConfig.sampleRatio;
                endpoint = cachedConfig.endpoint;
                batchReportMaxSize = cachedConfig.batchReportMaxSize;
                batchReportIntervalTime = cachedConfig.batchReportIntervalTime;
                maxReportsPerMinute = cachedConfig.maxReportsPerMinute;
                raw = cachedConfig.raw;
            } catch (JSONException e) {

            }
        }
    }

    @Override
    public void saveToCache(SharedPreferences.Editor editor) {
        if (raw != null) {
            editor.putString(Constants.CONFIG_OBSERVABLE_CONFIG, raw.toString());
        } else {
            editor.remove(Constants.CONFIG_OBSERVABLE_CONFIG);
        }
    }
}
