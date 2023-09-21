package com.alibaba.ams.emas.demo

import android.app.Application
import android.text.TextUtils
import com.alibaba.ams.emas.demo.constant.*
import com.alibaba.sdk.android.httpdns.InitConfig
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.aliyun.ams.httpdns.demo.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HttpDnsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!TextUtils.isEmpty(BuildConfig.ACCOUNT_ID)) {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.IO) {
                    val preferences = getAccountPreference(this@HttpDnsApplication)
                    val enableExpiredIp = preferences.getBoolean(KEY_ENABLE_EXPIRED_IP, false)
                    val enableCacheIp = preferences.getBoolean(KEY_ENABLE_CACHE_IP, false)
                    val enableHttpDns = preferences.getBoolean(KEY_ENABLE_HTTPS, false)
                    val timeout = preferences.getInt(KEY_TIMEOUT, 1500)
                    val region = preferences.getString(KEY_REGION, "cn")
                    //自定义ttl
                    val ttlCacheStr = preferences.getString(KEY_TTL_CHANGER, null)
                    TtlCacheHolder.convertTtlCacheData(ttlCacheStr)
                    //IP探测
                    val ipProbeItemJson = preferences.getString(KEY_IP_PROBE_ITEMS, null)
                    //主站域名
                    val hostListWithFixedIpJson =
                        preferences.getString(KEY_HOST_WITH_FIXED_IP, null)
                    //预解析
                    val preResolveHostList  = preferences.getString(KEY_PRE_RESOLVE_HOST_LIST, null)

                    InitConfig.Builder()
                        .setEnableHttps(enableHttpDns)
                        .setEnableCacheIp(enableCacheIp)
                        .setEnableExpiredIp(enableExpiredIp)
                        .setRegion(if (region.equals("cn")) null else region)
                        .setTimeout(timeout)
                        .setIpProbeItems(ipProbeItemJson.toIPProbeList())
                        .configCacheTtlChanger(TtlCacheHolder.cacheTtlChanger)
                        .configHostWithFixedIp(hostListWithFixedIpJson.toHostList())
                        .buildFor(BuildConfig.ACCOUNT_ID)

                    preResolveHostList?.let {
                        val dnsService = HttpDnsServiceHolder.getHttpDnsService(this@HttpDnsApplication)
                        dnsService?.setPreResolveHosts(it.toHostList() as ArrayList<String>, RequestIpType.both)
                    }
                }
            }
        }
    }
}