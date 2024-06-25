package com.alibaba.ams.emas.demo.net

import android.content.Context
import android.util.Log
import com.alibaba.ams.emas.demo.HttpDnsServiceHolder
import com.alibaba.ams.emas.demo.readStringFrom
import com.alibaba.ams.emas.demo.ui.resolve.Response
import com.alibaba.sdk.android.httpdns.HTTPDNSResult
import com.alibaba.sdk.android.httpdns.HttpDnsCallback
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

/**
 * @author allen.wy
 * @date 2023/5/26
 */
class HttpURLConnectionRequest(private val context: Context, private val requestIpType: RequestIpType, private val resolveMethod: String,
                               private val isSdns: Boolean, private val sdnsParams: Map<String, String>?, private val cacheKey: String): IRequest {

    override fun get(url: String): Response {
        val conn: HttpURLConnection = getConnection(url)
        val inputStream: InputStream?
        val streamReader: BufferedReader?
        return if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            inputStream = conn.errorStream
            var errStr: String? = null
            if (inputStream != null) {
                streamReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                errStr = readStringFrom(streamReader).toString()
            }
            throw Exception("Status Code : " + conn.responseCode + " Msg : " + errStr)
        } else {
            inputStream = conn.inputStream
            streamReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val responseStr: String = readStringFrom(streamReader).toString()
            Response(conn.responseCode, responseStr)
        }
    }

    private fun getConnection(url: String): HttpURLConnection {
        val host = URL(url).host
        val dnsService = HttpDnsServiceHolder.getHttpDnsService(context)

        var ipURL: String? = null
        dnsService?.let {
            //替换为最新的api
            var httpDnsResult = HTTPDNSResult("", null, null, null, false, false)
            if (resolveMethod == "getHttpDnsResultForHostSync(String host, RequestIpType type)") {
                httpDnsResult = if (isSdns) {
                    dnsService.getHttpDnsResultForHostSync(host, requestIpType, sdnsParams, cacheKey)
                } else {
                    dnsService.getHttpDnsResultForHostSync(host, requestIpType)
                }
            } else if (resolveMethod == "getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback)") {
                val lock = CountDownLatch(1)
                if (isSdns) {
                    dnsService.getHttpDnsResultForHostAsync(host, requestIpType, sdnsParams, cacheKey, HttpDnsCallback {
                        httpDnsResult = it
                        lock.countDown()
                    })
                } else {
                    dnsService.getHttpDnsResultForHostAsync(host, requestIpType, HttpDnsCallback {
                        httpDnsResult = it
                        lock.countDown()
                    })
                }
                lock.await()
            } else if (resolveMethod == "getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type)") {
                httpDnsResult = if (isSdns) {
                    dnsService.getHttpDnsResultForHostSyncNonBlocking(host, requestIpType, sdnsParams, cacheKey)
                } else {
                    dnsService.getHttpDnsResultForHostSyncNonBlocking(host, requestIpType)
                }
            }

            Log.d("httpdns", "httpdns $host 解析结果 $httpDnsResult")
            val ipStackType = HttpDnsNetworkDetector.getInstance().getNetType(context)
            val isV6 = ipStackType == NetType.v6 || ipStackType == NetType.both
            val isV4 = ipStackType == NetType.v4 || ipStackType == NetType.both

            if (httpDnsResult.ipv6s != null && httpDnsResult.ipv6s.isNotEmpty() && isV6) {
                ipURL = url.replace(host, "[" + httpDnsResult.ipv6s[0] + "]")
            } else if (httpDnsResult.ips != null && httpDnsResult.ips.isNotEmpty() && isV4) {
                ipURL = url.replace(host, httpDnsResult.ips[0])
            }
        }

        val conn: HttpURLConnection = URL(ipURL ?: url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Host", host)

        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = false

        if (conn is HttpsURLConnection) {
            val sslSocketFactory = TLSSNISocketFactory(conn)
            // SNI场景，创建SSLSocket
            conn.sslSocketFactory = sslSocketFactory
            // https场景，证书校验
            conn.hostnameVerifier = HostnameVerifier { _, session ->
                val requestHost = conn.getRequestProperty("Host") ?:conn.getURL().host
                HttpsURLConnection.getDefaultHostnameVerifier().verify(requestHost, session)
            }
        }

        val responseCode = conn.responseCode
        if (needRedirect(responseCode)) {
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
            return getConnection(location)
        }
        return conn
    }

    private fun needRedirect(code: Int): Boolean {
        return code in 300..399
    }

}