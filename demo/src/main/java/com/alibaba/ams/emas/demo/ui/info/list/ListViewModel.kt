package com.alibaba.ams.emas.demo.ui.info.list

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.ams.emas.demo.*
import com.alibaba.ams.emas.demo.TtlCacheHolder.toJsonString
import com.alibaba.ams.emas.demo.constant.KEY_BATCH_RESOLVE_HOST_LIST
import com.alibaba.ams.emas.demo.constant.KEY_HOST_BLACK_LIST
import com.alibaba.ams.emas.demo.constant.KEY_HOST_WITH_FIXED_IP
import com.alibaba.ams.emas.demo.constant.KEY_IP_RANKING_ITEMS
import com.alibaba.ams.emas.demo.constant.KEY_PRE_RESOLVE_HOST_LIST
import com.alibaba.ams.emas.demo.constant.KEY_TTL_CHANGER
import com.alibaba.sdk.android.httpdns.HttpDnsService
import com.alibaba.sdk.android.httpdns.ranking.IPRankingBean
import com.aliyun.ams.httpdns.demo.R
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @author allen.wy
 * @date 2023/6/6
 */
class ListViewModel(application: Application) : AndroidViewModel(application) {

    private var hostFixedIpList: MutableList<String> = mutableListOf()
    private var ipRankingList: MutableList<IPRankingBean> = mutableListOf()
    private var hostBlackList: MutableList<String> = mutableListOf()

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
                kListItemTypeBlackList -> {
                    val hostBlackListStr = preferences.getString(KEY_HOST_BLACK_LIST, null)
                    val list = hostBlackListStr.toBlackList()
                    list?.let {
                        hostBlackList.addAll(list)
                        for (host in hostBlackList) {
                            infoList.add(ListItem(kListItemTypeBlackList, host, 0))
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
                    for (host in PreResolveCacheHolder.preResolveV4List) {
                        infoList.add(ListItem(kListItemPreResolve, host, 0))
                    }

                    for (host in PreResolveCacheHolder.preResolveV6List) {
                        infoList.add(ListItem(kListItemPreResolve, host, 1))
                    }

                    for (host in PreResolveCacheHolder.preResolveBothList) {
                        infoList.add(ListItem(kListItemPreResolve, host, 2))
                    }

                    for (host in PreResolveCacheHolder.preResolveAutoList) {
                        infoList.add(ListItem(kListItemPreResolve, host, 3))
                    }
                }
                kListItemBatchResolve -> {
                    for (host in BatchResolveCacheHolder.batchResolveV4List) {
                        infoList.add(ListItem(kListItemBatchResolve, host, 0))
                    }
                    for (host in BatchResolveCacheHolder.batchResolveV6List) {
                        infoList.add(ListItem(kListItemBatchResolve, host, 1))
                    }
                    for (host in BatchResolveCacheHolder.batchResolveBothList) {
                        infoList.add(ListItem(kListItemBatchResolve, host, 2))
                    }
                    for (host in BatchResolveCacheHolder.batchResolveAutoList) {
                        infoList.add(ListItem(kListItemBatchResolve, host, 3))
                    }
                }
                else -> {
                    val ipRankingListStr = preferences.getString(KEY_IP_RANKING_ITEMS, null)
                    val rankingList = ipRankingListStr.toIPRankingList()
                    rankingList?.let {
                        ipRankingList.addAll(rankingList)
                        for (rankingItem in ipRankingList) {
                            infoList.add(
                                ListItem(
                                    kListItemTypeIPRanking,
                                    rankingItem.hostName,
                                    rankingItem.port
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

    fun toAddHostInBlackList(host: String, listAdapter: ListAdapter) {
        if (hostBlackList.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.host_black_list_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            hostBlackList.add(host)
            saveHostInBlackList()
            listAdapter.addItemData(
                ListItem(
                    kListItemTypeBlackList,
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

    private fun saveHostInBlackList() {
        viewModelScope.launch {
            val array = JSONArray()
            for (host in hostBlackList) {
                array.put(host)
            }

            preferences.edit()
                .putString(KEY_HOST_BLACK_LIST, array.toString())
                .apply()
        }
    }

    fun toSaveIPProbe(host: String, port: Int, listAdapter: ListAdapter) {
        val ipProbeItem =
            IPRankingBean(host, port)
        if (ipRankingList.contains(ipProbeItem)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.ip_probe_item_duplicate, host, port.toString()),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ipRankingList.add(ipProbeItem)
            saveIPProbe()
            listAdapter.addItemData(
                ListItem(
                    kListItemTypeIPRanking,
                    host,
                    port
                )
            )
        }
    }

    private fun saveIPProbe() {
        viewModelScope.launch {
            val jsonObject = JSONObject()
            for (item in ipRankingList) {
                try {
                    jsonObject.put(item.hostName, item.port)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            val ipProbeStr =  jsonObject.toString()
            val editor = preferences.edit()
            editor.putString(KEY_IP_RANKING_ITEMS, ipProbeStr)
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

    fun toAddPreResolveHost(host: String, listAdapter: ListAdapter, type: Int) {
        val list: MutableList<String> = when (type) {
            0 -> PreResolveCacheHolder.preResolveV4List
            1 -> PreResolveCacheHolder.preResolveV6List
            2 -> PreResolveCacheHolder.preResolveBothList
            else -> PreResolveCacheHolder.preResolveAutoList
        }

        if (list.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.pre_resolve_host_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            list.add(host)
            savePreResolveHost()
            listAdapter.addItemData(
                ListItem(
                    kListItemPreResolve,
                    host,
                    type
                )
            )
        }
    }

    fun toAddBatchResolveHost(host: String, listAdapter: ListAdapter, type: Int) {
        val list: MutableList<String> = when (type) {
            0 -> BatchResolveCacheHolder.batchResolveV4List
            1 -> BatchResolveCacheHolder.batchResolveV6List
            2 -> BatchResolveCacheHolder.batchResolveBothList
            else -> BatchResolveCacheHolder.batchResolveAutoList
        }
        if (list.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.batch_resolve_host_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            list.add(host)
            saveBatchResolveHost()
            listAdapter.addItemData(
                ListItem(
                    kListItemBatchResolve,
                    host,
                    type
                )
            )
        }
    }

    private fun savePreResolveHost() {
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_PRE_RESOLVE_HOST_LIST, PreResolveCacheHolder.convertPreResolveString())
            editor.apply()
        }
    }

    private fun saveBatchResolveHost() {
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_BATCH_RESOLVE_HOST_LIST, BatchResolveCacheHolder.convertBatchResolveString())
            editor.apply()
        }
    }

    fun onHostWithFixedIPDeleted(position: Int) {
        //只能重启生效
        val deletedHost = hostFixedIpList.removeAt(position)
        saveHostWithFixedIP()
    }

    fun onIPProbeItemDeleted(position: Int) {
        ipRankingList.removeAt(position)
        saveIPProbe()
    }

    fun onTtlDeleted(host: String) {
        TtlCacheHolder.ttlCache.remove(host)
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_TTL_CHANGER, TtlCacheHolder.ttlCache.toJsonString())
            editor.apply()
        }
    }

    fun onPreResolveDeleted(host: String, intValue: Int) {
        val list = when (intValue) {
            0 -> PreResolveCacheHolder.preResolveV4List
            1 -> PreResolveCacheHolder.preResolveV6List
            2 -> PreResolveCacheHolder.preResolveBothList
            else -> PreResolveCacheHolder.preResolveAutoList
        }
        list.remove(host)
        savePreResolveHost()
    }

    fun onBatchResolveDeleted(host: String, intValue: Int) {
        val list = when (intValue) {
            0 -> BatchResolveCacheHolder.batchResolveV4List
            1 -> BatchResolveCacheHolder.batchResolveV6List
            2 -> BatchResolveCacheHolder.batchResolveBothList
            else -> BatchResolveCacheHolder.batchResolveAutoList
        }
        list.remove(host)
        saveBatchResolveHost()
    }

    fun onHostBlackListDeleted(position: Int) {
        hostBlackList.removeAt(position)
        saveHostInBlackList()
    }

    private fun getString(resId: Int, vararg args: String): String {
        return getApplication<HttpDnsApplication>().getString(resId, *args)
    }
}