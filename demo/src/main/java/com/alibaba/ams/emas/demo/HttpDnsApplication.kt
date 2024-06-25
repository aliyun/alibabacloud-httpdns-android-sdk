package com.alibaba.ams.emas.demo

import android.app.Application
import android.text.TextUtils
import android.util.Log
import com.alibaba.ams.emas.demo.constant.KEY_BATCH_RESOLVE_HOST_LIST
import com.alibaba.ams.emas.demo.constant.KEY_ENABLE_CACHE_IP
import com.alibaba.ams.emas.demo.constant.KEY_ENABLE_EXPIRED_IP
import com.alibaba.ams.emas.demo.constant.KEY_ENABLE_HTTPS
import com.alibaba.ams.emas.demo.constant.KEY_ENABLE_LOG
import com.alibaba.ams.emas.demo.constant.KEY_HOST_BLACK_LIST
import com.alibaba.ams.emas.demo.constant.KEY_HOST_WITH_FIXED_IP
import com.alibaba.ams.emas.demo.constant.KEY_IP_RANKING_ITEMS
import com.alibaba.ams.emas.demo.constant.KEY_PRE_RESOLVE_HOST_LIST
import com.alibaba.ams.emas.demo.constant.KEY_REGION
import com.alibaba.ams.emas.demo.constant.KEY_SDNS_GLOBAL_PARAMS
import com.alibaba.ams.emas.demo.constant.KEY_TIMEOUT
import com.alibaba.ams.emas.demo.constant.KEY_TTL_CHANGER
import com.alibaba.sdk.android.httpdns.InitConfig
import com.alibaba.sdk.android.httpdns.NotUseHttpDnsFilter
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog
import com.aliyun.ams.httpdns.demo.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * @author allen.wy
 * @date 2023/5/24
 */
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
                    val timeout = preferences.getInt(KEY_TIMEOUT, 2000)
                    val region = preferences.getString(KEY_REGION, "cn")
                    //自定义ttl
                    val ttlCacheStr = preferences.getString(KEY_TTL_CHANGER, null)
                    TtlCacheHolder.convertTtlCacheData(ttlCacheStr)
                    //IP探测
                    val ipRankingItemJson = preferences.getString(KEY_IP_RANKING_ITEMS, null)
                    //主站域名
                    val hostListWithFixedIpJson =
                        preferences.getString(KEY_HOST_WITH_FIXED_IP, null)
                    //预解析
                    val preResolveHostList  = preferences.getString(KEY_PRE_RESOLVE_HOST_LIST, null)
                    preResolveHostList?.let { Log.d("httpdns:HttpDnsApplication", "pre resolve list: $it") }
                    PreResolveCacheHolder.convertPreResolveCacheData(preResolveHostList)
                    //批量解析
                    val batchResolveHostList = preferences.getString(KEY_BATCH_RESOLVE_HOST_LIST, null)
                    BatchResolveCacheHolder.convertBatchResolveCacheData(batchResolveHostList)

                    val sdnsGlobalParamStr = preferences.getString(KEY_SDNS_GLOBAL_PARAMS, "")
                    var sdnsGlobalParams: MutableMap<String, String>? = null
                    if (!TextUtils.isEmpty(sdnsGlobalParamStr)) {
                        try {
                            val sdnsJson = JSONObject(sdnsGlobalParamStr)
                            val keys = sdnsJson.keys()
                            sdnsGlobalParams = mutableMapOf()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                sdnsGlobalParams[key] = sdnsJson.getString(key)
                            }
                        } catch (e: JSONException) {

                        }
                    }

                    HttpDnsLog.enable(preferences.getBoolean(KEY_ENABLE_LOG, false))
                    InitConfig.Builder()
                        .setEnableHttps(enableHttpDns)
                        .setEnableCacheIp(enableCacheIp)
                        .setEnableExpiredIp(enableExpiredIp)
                        .setRegion(if (region.equals("cn")) null else region)
                        .setTimeoutMillis(timeout)
                        .setIPRankingList(ipRankingItemJson.toIPRankingList())
                        .configCacheTtlChanger(TtlCacheHolder.cacheTtlChanger)
                        .configHostWithFixedIp(hostListWithFixedIpJson.toHostList())
                        .setNotUseHttpDnsFilter(NotUseHttpDnsFilter {host ->
                            val blackListStr = preferences.getString(KEY_HOST_BLACK_LIST, null)
                            blackListStr?.let {
                                return@NotUseHttpDnsFilter blackListStr.contains(host)
                            }
                            return@NotUseHttpDnsFilter false
                        })
                        .setSdnsGlobalParams(sdnsGlobalParams)
                        .buildFor(BuildConfig.ACCOUNT_ID)

                    val dnsService = HttpDnsServiceHolder.getHttpDnsService(this@HttpDnsApplication)
                    dnsService?.setPreResolveHosts(PreResolveCacheHolder.preResolveV4List)
                    dnsService?.setPreResolveHosts(PreResolveCacheHolder.preResolveV6List, RequestIpType.v6)
                    dnsService?.setPreResolveHosts(PreResolveCacheHolder.preResolveBothList, RequestIpType.both)
                    dnsService?.setPreResolveHosts(PreResolveCacheHolder.preResolveAutoList, RequestIpType.auto)
                }
            }
        }
    }


}