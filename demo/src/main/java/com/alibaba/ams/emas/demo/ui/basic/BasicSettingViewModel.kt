package com.alibaba.ams.emas.demo.ui.basic

import android.app.Application
import android.text.TextUtils
import android.widget.CompoundButton
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.alibaba.ams.emas.demo.*
import com.alibaba.ams.emas.demo.constant.*
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.HttpDnsService
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog
import com.aliyun.ams.httpdns.demo.BuildConfig
import com.aliyun.ams.httpdns.demo.R


class BasicSettingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = getAccountPreference(getApplication())

    private var dnsService: HttpDnsService? = null

    /**
     * 是否允许过期IP
     */
    var enableExpiredIP = false

    /**
     * 是否开启本地缓存
     */
    var enableCacheIP = false

    /**
     * 是否允许HTTPS
     */
    var enableHttps = false

    /**
     * 是否开启降级
     */
    var enableDegrade = false

    /**
     * 是否允许网络切换自动刷新
     */
    var enableAutoRefresh = false

    /**
     * 是否允许打印日志
     */
    var enableLog = false

    /**
     * 当前Region
     */
    var currentRegion = SingleLiveData<String>().apply {
        value = ""
    }

    /**
     * 当前超时
     */
    var currentTimeout = SingleLiveData<String>().apply {
        value = "1500ms"
    }

    var showDialog: IBasicShowDialog? = null

    fun initData() {
        enableExpiredIP = preferences.getBoolean(KEY_ENABLE_EXPIRED_IP, false)
        enableCacheIP = preferences.getBoolean(KEY_ENABLE_CACHE_IP, false)
        enableHttps = preferences.getBoolean(KEY_ENABLE_HTTPS, false)
        enableDegrade = preferences.getBoolean(KEY_ENABLE_DEGRADE, false)
        enableAutoRefresh = preferences.getBoolean(KEY_ENABLE_AUTO_REFRESH, false)
        enableLog = preferences.getBoolean(KEY_ENABLE_LOG, false)
        when (preferences.getString(KEY_REGION, "cn")) {
            "cn" -> currentRegion.value = getString(R.string.china)
            "hk" -> currentRegion.value = getString(R.string.china_hk)
            "sg" -> currentRegion.value = getString(R.string.singapore)
        }
        currentTimeout.value = "${preferences.getInt(KEY_TIMEOUT, 1500)}ms"
        dnsService = HttpDnsServiceHolder.getHttpDnsService(getApplication())
    }


    fun toggleEnableExpiredIp(button: CompoundButton, checked: Boolean) {
        enableExpiredIP = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_EXPIRED_IP, checked)
        editor.apply()
        dnsService?.setExpiredIPEnabled(checked)
    }

    fun toggleEnableCacheIp(button: CompoundButton, checked: Boolean) {
        enableCacheIP = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_CACHE_IP, checked)
        editor.apply()
        dnsService?.setCachedIPEnabled(checked)
    }

    fun toggleEnableHttps(button: CompoundButton, checked: Boolean) {
        enableHttps = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_HTTPS, checked)
        editor.apply()
        dnsService?.setHTTPSRequestEnabled(checked)
    }

    fun toggleEnableDegrade(button: CompoundButton, checked: Boolean) {
        enableDegrade = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_DEGRADE, checked)
        editor.apply()
        dnsService?.setDegradationFilter { checked }
    }

    fun toggleEnableAutoRefresh(button: CompoundButton, checked: Boolean) {
        enableAutoRefresh = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_AUTO_REFRESH, checked)
        editor.apply()
        dnsService?.setPreResolveAfterNetworkChanged(checked)
    }

    fun toggleEnableLog(button: CompoundButton, checked: Boolean) {
        enableLog = checked
        val editor = preferences.edit()
        editor.putBoolean(KEY_ENABLE_LOG, checked)
        editor.apply()
        HttpDnsLog.enable(checked)
    }

    fun setRegion() {
        //弹窗选择region
        showDialog?.showSelectRegionDialog()
    }

    fun saveRegion(region: String) {
        currentRegion.value = when (region) {
            "cn" -> getString(R.string.china)
            "hk" -> getString(R.string.china_hk)
            "sg" -> getString(R.string.singapore)
            else -> getString(R.string.china)
        }
        val editor = preferences.edit()
        editor.putString(KEY_REGION, region)
        editor.apply()
        dnsService?.setRegion(if ("cn" == region) null else region)
    }

    fun setTimeout() {
        showDialog?.showSetTimeoutDialog()
    }

    fun saveTimeout(timeout: Int) {
        currentTimeout.value = "${timeout}ms"
        val editor = preferences.edit()
        editor.putInt(KEY_TIMEOUT, timeout)
        editor.apply()
        dnsService?.setTimeoutInterval(timeout)
    }

    fun showClearCacheDialog() {
        showDialog?.showInputHostDialog()
    }

    fun clearDnsCache(host: String) {
        dnsService?.cleanHostCache(mutableListOf(host) as ArrayList<String>)
    }

    fun showAddPreResolveDialog() {
        showDialog?.showAddPreResolveDialog()
    }

    fun addPreResolveDomain(host: String) {
        val preResolveHostListStr = preferences.getString(KEY_PRE_RESOLVE_HOST_LIST, null)
        val hostList: MutableList<String> = if (preResolveHostListStr == null) {
            mutableListOf()
        } else {
            preResolveHostListStr.toHostList()!!
        }

        if (hostList.contains(host)) {
            Toast.makeText(
                getApplication(),
                getString(R.string.pre_resolve_host_duplicate, host),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            hostList.add(host)
            dnsService?.setPreResolveHosts(hostList as ArrayList<String>, RequestIpType.both)
        }

        val editor = preferences.edit()
        editor.putString(KEY_PRE_RESOLVE_HOST_LIST, convertPreResolveList(hostList))
        editor.apply()
    }

    private fun getString(resId: Int): String {
        return getApplication<HttpDnsApplication>().getString(resId)
    }

    private fun getString(resId: Int, vararg args: String): String {
        return getApplication<HttpDnsApplication>().getString(resId, *args)
    }
}