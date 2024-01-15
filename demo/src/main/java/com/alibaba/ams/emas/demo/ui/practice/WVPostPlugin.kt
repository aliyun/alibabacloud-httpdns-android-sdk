package com.alibaba.ams.emas.demo.ui.practice

import android.taobao.windvane.jsbridge.WVApiPlugin
import android.taobao.windvane.jsbridge.WVCallBackContext
import android.taobao.windvane.jsbridge.WVResult
import com.alibaba.ams.emas.demo.net.OkHttpClientSingleton
import com.alibaba.sdk.android.httpdns.RequestIpType
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * @author allen.wy
 * @date 2023/6/14
 */
class WVPostPlugin : WVApiPlugin() {
    override fun execute(action: String?, param: String?, callback: WVCallBackContext?): Boolean {
        val jsonObj: JSONObject? = try {
            param?.let { JSONObject(it) }
        } catch (e: JSONException) {
            null
        }
        if (jsonObj == null) {
            callback?.error("params must be JSON which contains url and body")
        } else {
            val url = try {
                jsonObj.getString("url")
            } catch (e: Throwable) {
                null
            }

            if (url == null) {
                callback?.error("url can not be empty")
                return true
            }

            val headers = try {
                jsonObj.getJSONObject("header")
            } catch (e: Throwable) {
                null
            }
            val body = try {
                jsonObj.getJSONObject("body")
            } catch (e: Throwable) {
                null
            }

            if (body == null) {
                callback?.error("post body can not be empty")
                return true
            }

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaType(),
                body.toString()
            )
            val builder: Request.Builder = Request.Builder().url(url)

            headers?.let {
                headers.keys().forEach { key ->
                    builder.addHeader(key, headers.getString(key))
                }
            }

            val request = builder.post(requestBody).build()
            try {
                OkHttpClientSingleton.getInstance(mContext).updateConfig(RequestIpType.both, true)
                    .getOkHttpClient().newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            callback?.error(e.message)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val result = WVResult()
                            result.addData("success", true)
                            val data = JSONObject()
                            data.put("code", response.code)
                            data.put("response", response.body?.string())
                            result.addData("data", data)
                            callback?.success(result)
                        }

                    })
            } catch (e: Exception) {
                callback?.error(e.message)
            }
        }
        return true
    }
}