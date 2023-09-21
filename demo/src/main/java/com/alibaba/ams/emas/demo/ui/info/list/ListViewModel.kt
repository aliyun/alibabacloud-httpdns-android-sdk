package com.alibaba.ams.emas.demo.ui.info.list

import android.app.Application
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.ams.emas.demo.*
import com.alibaba.ams.emas.demo.TtlCacheHolder.toJsonString
import com.alibaba.ams.emas.demo.constant.KEY_HOST_WITH_FIXED_IP
import com.alibaba.ams.emas.demo.constant.KEY_IP_PROBE_ITEMS
import com.alibaba.ams.emas.demo.constant.KEY_PRE_RESOLVE_HOST_LIST
import com.alibaba.ams.emas.demo.constant.KEY_TTL_CHANGER
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.HttpDnsService
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem
import com.aliyun.ams.httpdns.demo.BuildConfig
import com.aliyun.ams.httpdns.demo.R
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private var hostFixedIpList: MutableList<String> = mutableListOf()
    private var ipProbeList: MutableList<IPProbeItem> = mutableListOf()
    private var preResolveHostList: MutableList<String> = mutableListOf()

    private var dnsService: HttpDnsService? = null

    private lateinit var preferences: SharedPreferences

    fun initData(listType: Int, infoList: MutableList<ListItem>) {
        dnsService = HttpDnsServiceHolder.getHttpDnsService(getApplication())
        preferences = getAccountPreference(getApplication())
        viewModelScope.launch {
            when (listType) {
                kListItemTypeHostWithFixedIP -> {
                    val hostFixedIpStr = preferences.getString(KEY_HOST_WITH_FIXED_IP, null)
                    val list = hostFixedIpStr.toHostList()
                    list?.let {
                        hostFixedIpList.addAll(list)
                        for (host in hostFixedIpList) {
                            infoList.add(ListItem(kListItemTypeHostWithFixedIP, host, 0))
                        }
                    }
                }
                kListItemTypeCacheTtl -> {
                    val ttlCacheStr = preferences.getString(KEY_TTL_CHANGER, null)
                    val map = ttlCacheStr.toTtlCacheMap()
                    map?.let {
                        TtlCacheHolder.ttlCache.putAll(map)
                        for ((host, ttl) in TtlCacheHolder.ttlCache) {
                            infoList.add(ListItem(kListItemTypeCacheTtl, host, ttl))
                        }
                    }
                }
                kListItemPreResolve -> {
                    val preResolveHostStr = preferences.getString(KEY_PRE_RESOLVE_HOST_LIST, null)
                    val list = preResolveHostStr.toHostList()
                    list?.let {
                        preResolveHostList.addAll(list)
                        for (host in preResolveHostList) {
                            infoList.add(ListItem(kListItemPreResolve, host, 0))
                        }
                    }
                }
                else -> {
                    val ipProbeListStr = preferences.getString(KEY_IP_PROBE_ITEMS, null)
                    val probeList = ipProbeListStr.toIPProbeList()
                    probeList?.let {
                        ipProbeList.addAll(probeList)
                        for (probeItem in ipProbeList) {
                            infoList.add(
                                ListItem(
                                    kListItemTypeIPProbe,
                                    probeItem.hostName,
                                    probeItem.port
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun toAddHostWithFixedIP(host: String, listAdapter: ListAdapter) {
        if (hostFixedIpList.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.host_fixed_ip_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            hostFixedIpList.add(host)
            saveHostWithFixedIP()
            listAdapter.addItemData(
                ListItem(
                    kListItemTypeHostWithFixedIP,
                    host,
                    0
                )
            )
        }
    }

    private fun saveHostWithFixedIP() {
        viewModelScope.launch {
            val array = JSONArray()
            for (host in hostFixedIpList) {
                array.put(host)
            }
            val hostStr = array.toString()
            val editor = preferences.edit()
            editor.putString(KEY_HOST_WITH_FIXED_IP, hostStr)
            editor.apply()
        }
    }

    fun toSaveIPProbe(host: String, port: Int, listAdapter: ListAdapter) {
        val ipProbeItem = IPProbeItem(host, port)
        if (ipProbeList.contains(ipProbeItem)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.ip_probe_item_duplicate, host, port.toString()),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ipProbeList.add(ipProbeItem)
            saveIPProbe()
            listAdapter.addItemData(
                ListItem(
                    kListItemTypeIPProbe,
                    host,
                    port
                )
            )
            dnsService?.setIPProbeList(ipProbeList)
        }
    }

    private fun saveIPProbe() {
        viewModelScope.launch {
            val jsonObject = JSONObject()
            for (item in ipProbeList) {
                try {
                    jsonObject.put(item.hostName, item.port)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            val ipProbeStr =  jsonObject.toString()
            val editor = preferences.edit()
            editor.putString(KEY_IP_PROBE_ITEMS, ipProbeStr)
            editor.apply()
        }
    }

    fun toSaveTtlCache(host: String, ttl: Int, listAdapter: ListAdapter) {
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_TTL_CHANGER, TtlCacheHolder.ttlCache.toJsonString())
            editor.apply()
        }
        if (TtlCacheHolder.ttlCache.containsKey(host)) {
            val position = listAdapter.getPositionByContent(host)
            if (position != -1) {
                listAdapter.updateItemByPosition(host, ttl, position)
            }
        } else {
            listAdapter.addItemData(
                ListItem(kListItemTypeCacheTtl, host, ttl)
            )
        }
        TtlCacheHolder.ttlCache[host] = ttl
    }

    fun toAddPreResolveHost(host: String, listAdapter: ListAdapter) {
        if (preResolveHostList.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.pre_resolve_host_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            preResolveHostList.add(host)
            savePreResolveHost()
            listAdapter.addItemData(
                ListItem(
                    kListItemPreResolve,
                    host,
                    0
                )
            )
            Log.d("httpdns", "before set pre")
            dnsService?.setPreResolveHosts(preResolveHostList as ArrayList<String>?, RequestIpType.both)
        }
    }

    private fun savePreResolveHost() {
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_PRE_RESOLVE_HOST_LIST, convertPreResolveList(preResolveHostList))
            editor.apply()
        }
    }

    fun onHostWithFixedIPDeleted(position: Int) {
        //只能重启生效
        val deletedHost = hostFixedIpList.removeAt(position)
        saveHostWithFixedIP()
    }

    fun onIPProbeItemDeleted(position: Int) {
        ipProbeList.removeAt(position)
        saveIPProbe()
        dnsService?.setIPProbeList(ipProbeList)
    }

    fun onTtlDeleted(host: String) {
        TtlCacheHolder.ttlCache.remove(host)
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_TTL_CHANGER, TtlCacheHolder.ttlCache.toJsonString())
            editor.apply()
        }
    }

    fun onPreResolveDeleted(position: Int) {
        preResolveHostList.removeAt(position)
        savePreResolveHost()
        dnsService?.setPreResolveHosts(preResolveHostList as ArrayList<String>?, RequestIpType.both)
    }

    private fun getString(resId: Int, vararg args: String): String {
        return getApplication<HttpDnsApplication>().getString(resId, *args)
    }
}