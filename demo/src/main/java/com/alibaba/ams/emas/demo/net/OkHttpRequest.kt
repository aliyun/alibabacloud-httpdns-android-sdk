package com.alibaba.ams.emas.demo.net

import android.content.Context
import com.alibaba.ams.emas.demo.ui.resolve.Response
import com.alibaba.sdk.android.httpdns.RequestIpType

/**
 * @author allen.wy
 * @date 2023/5/25
 */
class OkHttpRequest  constructor(
    private val context: Context,
    private val requestIpType: RequestIpType,
    private val resolveMethod: String,
    private val mIsSdns: Boolean,
    private val mSdnsParams: Map<String, String>?,
    private val mCacheKey: String
) : IRequest {

    override fun get(url: String): Response {
        val request = okhttp3.Request.Builder().url(url).build()
        OkHttpClientSingleton.getInstance(context).updateConfig(requestIpType, resolveMethod, mIsSdns, mSdnsParams, mCacheKey).getOkHttpClient().newCall(request).execute()
            .use { response -> return Response(response.code, response.body?.string()) }
    }

}