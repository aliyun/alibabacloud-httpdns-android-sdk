package com.alibaba.ams.emas.demo.ui.practice

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.ams.emas.demo.HttpDnsServiceHolder
import com.alibaba.ams.emas.demo.net.TLSSNISocketFactory
import com.alibaba.ams.emas.demo.readStringFrom
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import com.alibaba.sdk.android.tool.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

/**
 * @author allen.wy
 * @date 2023/6/15
 */
class BestPracticeViewModel(application: Application) : AndroidViewModel(application) {


    var showDialog: IBestPracticeShowDialog? = null


    fun sniRequest() {
        if (!NetworkUtils.isNetworkConnected(getApplication())) {
            showDialog?.showNoNetworkDialog()
            return
        }
        val testUrl = "https://suggest.taobao.com/sug?code=utf-8&q=phone"
        viewModelScope.launch(Dispatchers.IO) {
            recursiveRequest(testUrl) { message ->
                withContext(Dispatchers.Main) {
                    showDialog?.showResponseDialog(
                        message
                    )
                }
            }
        }
    }


    private suspend fun recursiveRequest(url: String, callback: suspend (message: String) -> Unit) {
        val host = URL(url).host
        var ipURL: String? = null
        val dnsService = HttpDnsServiceHolder.getHttpDnsService(getApplication())
        dnsService?.let {
            val httpDnsResult = dnsService.getIpsByHostAsync(host, RequestIpType.both)
            Log.d("httpdns", "$host 解析结果 $httpDnsResult")
            val ipStackType = HttpDnsNetworkDetector.getInstance().getNetType(getApplication())
            val isV6 = ipStackType == NetType.v6 || ipStackType == NetType.both
            val isV4 = ipStackType == NetType.v4 || ipStackType == NetType.both

            if (httpDnsResult.ipv6s != null && httpDnsResult.ipv6s.isNotEmpty() && isV6) {
                ipURL = url.replace(host, "[" + httpDnsResult.ipv6s[0] + "]")
            } else if (httpDnsResult.ips != null && httpDnsResult.ips.isNotEmpty() && isV4) {
                ipURL = url.replace(host, httpDnsResult.ips[0])
            }
        }

        val conn: HttpsURLConnection =
            URL(ipURL ?: url).openConnection() as HttpsURLConnection
        conn.setRequestProperty("Host", host)
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = false

        //设置SNI
        val sslSocketFactory = TLSSNISocketFactory(conn)
        // SNI场景，创建SSLSocket
        conn.sslSocketFactory = sslSocketFactory
        conn.hostnameVerifier = HostnameVerifier { _, session ->
            val requestHost = conn.getRequestProperty("Host") ?: conn.url.host
            HttpsURLConnection.getDefaultHostnameVerifier().verify(requestHost, session)
        }
        val code = conn.responseCode
        if (needRedirect(code)) {
            //临时重定向和永久重定向location的大小写有区分
            var location = conn.getHeaderField("Location")
            if (location == null) {
                location = conn.getHeaderField("location")
            }
            if (!(location!!.startsWith("http://") || location.startsWith("https://"))) {
                //某些时候会省略host，只返回后面的path，所以需要补全url
                val originalUrl = URL(url)
                location = (originalUrl.protocol + "://"
                        + originalUrl.host + location)
            }
            recursiveRequest(location, callback)
        } else {
            val inputStream: InputStream?
            val streamReader: BufferedReader?
            if (code != HttpURLConnection.HTTP_OK) {
                inputStream = conn.errorStream
                var errMsg: String? = null
                if (inputStream != null) {
                    streamReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                    errMsg = readStringFrom(streamReader).toString()
                }
                Log.d("httpdns", "SNI request error: $errMsg")
                callback("$code - $errMsg")
            } else {
                inputStream = conn.inputStream
                streamReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val body: String = readStringFrom(streamReader).toString()
                Log.d("httpdns", "SNI request response: $body")
                callback("$code - $body")
            }
        }
    }

    private fun needRedirect(code: Int): Boolean {
        return code in 300..399
    }

}