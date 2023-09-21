package com.alibaba.ams.emas.demo

import android.content.Context
import android.text.TextUtils
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.HttpDnsService
import com.aliyun.ams.httpdns.demo.BuildConfig

object HttpDnsServiceHolder {

    fun getHttpDnsService(context: Context) : HttpDnsService? {
        val dnsService = if (!TextUtils.isEmpty(BuildConfig.ACCOUNT_ID)) {
            if (!TextUtils.isEmpty(BuildConfig.SECRET_KEY)) HttpDns.getService(
                context,
                BuildConfig.ACCOUNT_ID, BuildConfig.SECRET_KEY
            ) else HttpDns.getService(
                context,
                BuildConfig.ACCOUNT_ID
            )
        } else null

        return dnsService
    }

}