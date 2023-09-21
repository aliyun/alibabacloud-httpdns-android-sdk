package com.alibaba.ams.emas.demo.net

import android.content.Context
import android.util.Log
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.SyncService
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import com.aliyun.ams.httpdns.demo.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

 class OkHttpClientSingleton private constructor(context: Context,
                                                 private val requestIpType: RequestIpType,
                                                 private val async: Boolean
) {

    private val context = WeakReference(context)

    private val tag: String = "OkHttpClientSingleton"

    companion object {
        @Volatile
        private var instance: OkHttpClientSingleton? = null

        fun getInstance(context: Context, requestIpType: RequestIpType,
                        async: Boolean): OkHttpClientSingleton {
            if (instance != null) {
                return instance!!
            }

            return synchronized(this) {
                if (instance != null) {
                    instance!!
                } else {
                    instance = OkHttpClientSingleton(context, requestIpType, async)
                    instance!!
                }
            }
        }
    }

    fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(0, 10 * 1000, TimeUnit.MICROSECONDS))
            .hostnameVerifier { _, _ ->true }
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val dnsService = HttpDns.getService(context.get(), BuildConfig.ACCOUNT_ID)
                    val httpDnsResult =
                        if (async) dnsService.getIpsByHostAsync(hostname, requestIpType) else
                            (dnsService as SyncService).getByHost(hostname, requestIpType)

                    Log.d(tag, "httpdns $hostname 解析结果 $httpDnsResult")
                    // 这里需要根据实际情况选择使用ipv6地址 还是 ipv4地址， 下面示例的代码优先使用了ipv6地址
                    val ipStackType = HttpDnsNetworkDetector.getInstance().getNetType(context.get())
                    val inetAddresses = mutableListOf<InetAddress>()
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

                    if (inetAddresses.isEmpty()) {
                        Log.d(tag, "httpdns 未返回IP，走local dns")
                        return Dns.SYSTEM.lookup(hostname)
                    }
                    return inetAddresses
                }
            })
            .build()
    }


}
