package com.alibaba.ams.emas.demo.net

import android.content.Context
import android.util.Log
import com.alibaba.ams.emas.demo.ui.resolve.Response
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.SyncService
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import com.aliyun.ams.httpdns.demo.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class OkHttpRequest  constructor(
    private val context: Context,
    private val requestIpType: RequestIpType,
    private val async: Boolean
) : IRequest {

    override fun get(url: String): Response {
        val request = okhttp3.Request.Builder().url(url).build()
        OkHttpClientSingleton.getInstance(context, requestIpType, async).getOkHttpClient().newCall(request).execute()
            .use { response -> return Response(response.code, response.body?.string()) }
    }

}