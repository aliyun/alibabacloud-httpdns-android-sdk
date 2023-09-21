package com.alibaba.ams.emas.demo.ui.info

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.alibaba.ams.emas.demo.HttpDnsApplication
import com.alibaba.ams.emas.demo.SingleLiveData
import com.alibaba.ams.emas.demo.getAccountPreference
import com.alibaba.sdk.android.httpdns.NetType
import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector
import com.aliyun.ams.httpdns.demo.BuildConfig
import com.aliyun.ams.httpdns.demo.R


class InfoViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * 账户ID
     */
    val accountId = SingleLiveData<String>().apply {
        value = ""
    }

    /**
     * 账户secret
     */
    val secretKey = SingleLiveData<String?>()

    val currentIpStackType = SingleLiveData<String>().apply {
        value = "V4"
    }

    fun initData() {
        currentIpStackType.value = when (HttpDnsNetworkDetector.getInstance().getNetType(getApplication())) {
            NetType.v4 -> "V4"
            NetType.v6 -> "V6"
            NetType.both -> "V4&V6"
            else -> getApplication<HttpDnsApplication>().getString(R.string.unknown)
        }

        accountId.value = BuildConfig.ACCOUNT_ID
        secretKey.value = BuildConfig.SECRET_KEY
    }

    fun clearAllCache() {
        val preferences = getAccountPreference(getApplication())
        preferences.edit().clear().apply()
        Toast.makeText(getApplication(), R.string.all_cache_cleared, Toast.LENGTH_SHORT).show()
    }


}