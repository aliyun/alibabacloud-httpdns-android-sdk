package com.alibaba.sdk.android.httpdns.test.server.base;

import java.util.List;

/**
 * 业务服务对外提供的接口,
 * 包括 预设请求结果；查询请求结果
 * 条件维度有：参数、个数（查前几个）
 * 效果维度有：生效次数、是否移除
 * 结果维度有：超时，成功，失败
 * @author zonglin.nzl
 * @date 2020/12/5
 */
public interface ServerApi<ARG, DATA> {

    /**
     * 预置请求超时
     */
    void preSetRequestTimeout(ARG arg, int count);

    /**
     * 预置请求返回结果
     *
     * @param arg
     * @param data
     * @param count 使用次数
     */
    void preSetRequestResponse(ARG arg, DATA data, int count);

    /**
     * 预置请求返回结果
     *
     * @param arg
     * @param httpCode
     * @param body
     * @param count    使用次数
     */
    void preSetRequestResponse(ARG arg, int httpCode, String body, int count);

    /**
     * 判断是否请求
     *
     * @param arg
     * @param count
     * @param removeRecord 是否移除记录
     * @return
     */
    boolean hasRequestForArg(ARG arg, int count, boolean removeRecord);

    /**
     * 判断是否请求
     *
     * @param arg
     * @param params       query参数
     * @param count
     * @param removeRecord 是否移除记录
     * @return
     */
    boolean hasRequestForArgWithParams(ARG arg, List<String> params, int count, boolean removeRecord);

    /**
     * 判断是否请求超时
     *
     * @param arg
     * @param count
     * @param removeRecord 是否移除记录
     * @return
     */
    boolean hasRequestForArgTimeout(ARG arg, int count, boolean removeRecord);

    /**
     * 判断是否有count个返回data的请求
     *
     * @param arg
     * @param data
     * @param count
     * @param removeRecord 是否移除记录
     * @return
     */
    boolean hasRequestForArgWithResult(ARG arg, DATA data, int count, boolean removeRecord);


    /**
     * 判断是否请求过
     *
     * @param arg
     * @param httpCode
     * @param body
     * @param count
     * @param removeRecord 是否移除记录
     * @return
     */
    boolean hasRequestForArgWithResult(ARG arg, int httpCode, String body, int count, boolean removeRecord);

    /**
     * 获取请求的结果
     * @param arg
     * @param count
     * @param removeRecord
     * @return
     */
    List<DATA> getResponse(ARG arg, int count, boolean removeRecord);


    /**
     * 清除服务目前的所有请求记录
     */
    void cleanRecord();

}
