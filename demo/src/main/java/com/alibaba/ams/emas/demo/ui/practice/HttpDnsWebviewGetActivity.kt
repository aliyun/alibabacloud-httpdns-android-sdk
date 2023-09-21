package com.alibaba.ams.emas.demo.ui.practice

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.ams.emas.demo.HttpDnsServiceHolder
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.ActivityHttpDnsWebviewBinding
import java.io.IOException
import java.net.*
import javax.net.ssl.*


class HttpDnsWebviewGetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHttpDnsWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHttpDnsWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webviewToolbar.title = getString(R.string.httpdns_webview_best_practice)
        setSupportActionBar(binding.webviewToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//添加默认的返回图标
        supportActionBar?.setHomeButtonEnabled(true)

        binding.httpdnsWebview.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {

                val url = request?.url.toString()
                val schema = request?.url?.scheme?.trim()
                val method = request?.method
                if ("get" != method && "GET" != method) {
                    return super.shouldInterceptRequest(view, request)
                }

                schema?.let {
                    if (!schema.startsWith("https") && !schema.startsWith("http")) {
                        return super.shouldInterceptRequest(view, request)
                    }
                    val headers = request.requestHeaders
                    try {
                        val urlConnection = recursiveRequest(url, headers)
                            ?: return super.shouldInterceptRequest(view, request)

                        val contentType = urlConnection.contentType
                        val mimeType = contentType?.split(";")?.get(0)
                        if (TextUtils.isEmpty(mimeType)) {
                            //无mimeType得请求不拦截
                            return super.shouldInterceptRequest(view, request)
                        }
                        val charset = getCharset(contentType)
                        val httpURLConnection = urlConnection as HttpURLConnection
                        val statusCode = httpURLConnection.responseCode
                        var response = httpURLConnection.responseMessage
                        val headerFields = httpURLConnection.headerFields
                        val isBinaryResource =
                            mimeType!!.startsWith("image") || mimeType.startsWith("audio") || mimeType.startsWith(
                                "video"
                            )
                        if (!TextUtils.isEmpty(charset) || isBinaryResource) {
                            val resourceResponse = WebResourceResponse(
                                mimeType,
                                charset,
                                httpURLConnection.inputStream
                            )
                            if (TextUtils.isEmpty(response)) {
                                response = "OK"
                            }
                            resourceResponse.setStatusCodeAndReasonPhrase(statusCode, response)
                            val responseHeader: MutableMap<String?, String> = HashMap()
                            for ((key) in headerFields) {
                                // HttpUrlConnection可能包含key为null的报头，指向该http请求状态码
                                responseHeader[key] = httpURLConnection.getHeaderField(key)
                            }
                            resourceResponse.responseHeaders = responseHeader
                            return resourceResponse
                        } else {
                            return super.shouldInterceptRequest(view, request)
                        }
                    } catch (e: Exception) {
                        Log.e("httpdns", Log.getStackTraceString(e))
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }
        }
        binding.httpdnsWebview.loadUrl("https://www.aliyun.com")
    }

    private fun getCharset(contentType: String?): String? {
        if (contentType == null) {
            return null
        }
        val fields = contentType.split(";")
        if (fields.size <= 1) {
            return null
        }
        var charset = fields[1]
        if (!charset.contains("=")) {
            return null
        }
        charset = charset.substring(charset.indexOf("=") + 1)
        return charset
    }

    private fun recursiveRequest(path: String, headers: Map<String, String>?): URLConnection? {
        try {
            val url = URL(path)
            val httpdnsService = HttpDnsServiceHolder.getHttpDnsService(this@HttpDnsWebviewGetActivity)
                ?: return null
            val hostIP: String? = httpdnsService.getIpByHostAsync(url.host) ?: return null
            val newUrl = if (hostIP == null) path else path.replaceFirst(url.host, hostIP)
            val urlConnection: HttpURLConnection = URL(newUrl).openConnection() as HttpURLConnection
            if (headers != null) {
                for ((key, value) in headers) {
                    urlConnection.setRequestProperty(key, value)
                }
            }
            urlConnection.setRequestProperty("Host", url.host)
            urlConnection.connectTimeout = 30000
            urlConnection.readTimeout = 30000
            urlConnection.instanceFollowRedirects = false
            if (urlConnection is HttpsURLConnection) {
                val sniFactory = SNISocketFactory(urlConnection)
                urlConnection.sslSocketFactory = sniFactory
                urlConnection.hostnameVerifier = HostnameVerifier { _, session ->
                    var host: String? = urlConnection.getRequestProperty("Host")
                    if (null == host) {
                        host = urlConnection.getURL().host
                    }
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session)
                }
            }

            val responseCode = urlConnection.responseCode
            if (responseCode in 300..399) {
                if (containCookie(headers)) {
                    return null
                }

                var location: String? = urlConnection.getHeaderField("Location")
                if (location == null) {
                    location = urlConnection.getHeaderField("location")
                }

                return if (location != null) {
                    if (!(location.startsWith("http://") || location.startsWith("https://"))) {
                        //某些时候会省略host，只返回后面的path，所以需要补全url
                        val originalUrl = URL(path)
                        location = (originalUrl.protocol + "://" + originalUrl.host + location)
                    }
                    recursiveRequest(location, headers)
                } else {
                    null
                }
            } else {
                return urlConnection
            }
        } catch (e: MalformedURLException) {
            Log.e("httpdns", Log.getStackTraceString(e))
        } catch (e: IOException) {
            Log.e("httpdns", Log.getStackTraceString(e))
        }
        return null
    }

    private fun containCookie(headers: Map<String, String>?): Boolean {
        if (headers == null) {
            return false
        }
        for ((key) in headers) {
            if (key.contains("Cookie")) {
                return true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}