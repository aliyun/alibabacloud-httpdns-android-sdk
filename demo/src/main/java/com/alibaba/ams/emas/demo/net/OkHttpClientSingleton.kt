package com.alibaba.ams.emas.demo.net

import android.content.Context
import android.util.Log
import com.alibaba.ams.emas.demo.HttpDnsServiceHolder
import com.alibaba.sdk.android.httpdns.HTTPDNSResult
import com.alibaba.sdk.android.httpdns.HttpDnsCallback
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author allen.wy
 * @date 2023/6/14
 */
 class OkHttpClientSingleton private constructor(context: Context
) {

    private val mContext = WeakReference(context)

    private var mRequestIpType = RequestIpType.v4
    private var mResolveMethod: String = "getHttpDnsResultForHostSync(String host, RequestIpType type)"
    private var mIsSdns: Boolean = false
    private var mSdnsParams: Map<String, String>? = null
    private var mCacheKey: String? = null

    private val tag: String = "httpdns:hOkHttpClientSingleton"

    companion object {
        @Volatile
        private var instance: OkHttpClientSingleton? = null

        fun getInstance(context: Context): OkHttpClientSingleton {
            if (instance != null) {
                return instance!!
            }

            return synchronized(this) {
                if (instance != null) {
                    instance!!
                } else {
                    instance = OkHttpClientSingleton(context)
                    instance!!
                }
            }
        }
    }

    fun updateConfig(requestIpType: RequestIpType, resolveMethod: String, isSdns: Boolean, params: Map<String, String>?, cacheKey: String): OkHttpClientSingleton {
        mRequestIpType = requestIpType
        mResolveMethod = resolveMethod
        mIsSdns = isSdns
        mSdnsParams = params
        mCacheKey = cacheKey
        return this
    }

    fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor(OkHttpLog())
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(0, 10 * 1000, TimeUnit.MICROSECONDS))
            .hostnameVerifier { _, _ ->true }
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    Log.d(tag, "start lookup $hostname via $mResolveMethod")
                    val dnsService = HttpDnsServiceHolder.getHttpDnsService(mContext.get()!!)
                    //修改为最新的通俗易懂的api
                    var httpDnsResult: HTTPDNSResult? = null
                    val inetAddresses = mutableListOf<InetAddress>()
                    if (mResolveMethod == "getHttpDnsResultForHostSync(String host, RequestIpType type)") {
                        httpDnsResult = if (mIsSdns) {
                            dnsService?.getHttpDnsResultForHostSync(hostname, mRequestIpType, mSdnsParams, mCacheKey)
                        } else {
                            dnsService?.getHttpDnsResultForHostSync(hostname, mRequestIpType)
                        }
                    } else if (mResolveMethod == "getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback)") {
                        val lock = CountDownLatch(1)
                        if (mIsSdns) {
                            dnsService?.getHttpDnsResultForHostAsync(
                                hostname,
                                mRequestIpType,
                                mSdnsParams,
                                mCacheKey,
                                HttpDnsCallback {
                                    httpDnsResult = it
                                    lock.countDown()
                                })
                        } else {
                            dnsService?.getHttpDnsResultForHostAsync(
                                hostname,
                                mRequestIpType,
                                HttpDnsCallback {
                                    httpDnsResult = it
                                    lock.countDown()
                                })
                        }
                        lock.await()
                    } else if (mResolveMethod == "getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type)") {
                        httpDnsResult = if (mIsSdns) {
                            dnsService?.getHttpDnsResultForHostSyncNonBlocking(hostname, mRequestIpType, mSdnsParams,  mCacheKey)
                        } else {
                            dnsService?.getHttpDnsResultForHostSyncNonBlocking(hostname, mRequestIpType)
                        }
                    }

                    Log.d(tag, "httpdns $hostname 解析结果 $httpDnsResult")
                    httpDnsResult?.let { processDnsResult(it, inetAddresses) }

                    if (inetAddresses.isEmpty()) {
                        Log.d(tag, "httpdns 未返回IP，走local dns")
                        return Dns.SYSTEM.lookup(hostname)
                    }
                    return inetAddresses
                }
            })
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }

    fun processDnsResult(httpDnsResult: HTTPDNSResult, inetAddresses: MutableList<InetAddress>) {
        val ipStackType = HttpDnsNetworkDetector.getInstance().getNetType(mContext.get())
        val isV6 = ipStackType == NetType.v6 || ipStackType == NetType.both
        val isV4 = ipStackType == NetType.v4 || ipStackType == NetType.both

        if (httpDnsResult.ipv6s != null && httpDnsResult.ipv6s.isNotEmpty() && isV6) {
            for (i in httpDnsResult.ipv6s.indices) {
                inetAddresses.addAll(
                    InetAddress.getAllByName(httpDnsResult.ipv6s[i]).toList()
                )
            }
        } else if (httpDnsResult.ips != null && httpDnsResult.ips.isNotEmpty() && isV4) {
            for (i in httpDnsResult.ips.indices) {
                inetAddresses.addAll(
                    InetAddress.getAllByName(httpDnsResult.ips[i]).toList()
                )
            }
        }
    }
}
