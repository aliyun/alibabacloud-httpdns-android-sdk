package com.alibaba.ams.emas.demo.ui.resolve

import android.app.Application
import android.util.Log
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.ams.emas.demo.HttpDnsApplication
import com.alibaba.ams.emas.demo.SingleLiveData
import com.alibaba.ams.emas.demo.constant.KEY_ASYNC_RESOLVE
import com.alibaba.ams.emas.demo.constant.KEY_RESOLVE_IP_TYPE
import com.alibaba.ams.emas.demo.getAccountPreference
import com.alibaba.ams.emas.demo.net.HttpURLConnectionRequest
import com.alibaba.ams.emas.demo.net.OkHttpRequest
import com.alibaba.sdk.android.httpdns.RequestIpType
import com.aliyun.ams.httpdns.demo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ResolveViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = getAccountPreference(getApplication())

    val asyncResolve = SingleLiveData<Boolean>().apply {
        value = true
    }

    val currentIpType = SingleLiveData<String>().apply {
        value = "IPv4"
    }

    var showDialog:IResolveShowDialog? = null

    private var requestType: NetRequestType = NetRequestType.OKHTTP
    private var schemaType: SchemaType = SchemaType.HTTPS

    fun initData() {
        asyncResolve.value = preferences.getBoolean(KEY_ASYNC_RESOLVE, true)
        val ipType = preferences.getString(KEY_RESOLVE_IP_TYPE, "IPv4")
        currentIpType.value = when(ipType) {
            "Auto" -> getApplication<HttpDnsApplication>().getString(R.string.auto_get_ip_type)
            else -> ipType
        }

    }

    fun onNetRequestTypeChanged(radioGroup: RadioGroup, id: Int) {
        requestType = when(id) {
            R.id.http_url_connection -> NetRequestType.HTTP_URL_CONNECTION
            else -> NetRequestType.OKHTTP
        }
    }

    fun toggleEnableAsync(button: CompoundButton, checked: Boolean) {
        asyncResolve.value = checked
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putBoolean(KEY_ASYNC_RESOLVE, checked)
            editor.apply()
        }

    }

    fun onSchemaTypeChanged(radioGroup: RadioGroup, id: Int) {
        schemaType = when(id) {
            R.id.schema_http -> SchemaType.HTTP
            else -> SchemaType.HTTPS
        }
    }

    fun setResolveIpType() {
        showDialog?.showSelectResolveIpTypeDialog()
    }

    fun saveResolveIpType(ipType: String) {
        viewModelScope.launch {
            val editor = preferences.edit()
            editor.putString(KEY_RESOLVE_IP_TYPE, ipType)
            editor.apply()
        }
        currentIpType.value = when (ipType) {
            "Auto" -> getApplication<HttpDnsApplication>().getString(R.string.auto_get_ip_type)
            else -> ipType
        }
    }

    fun startToResolve(host: String, api: String) {
        val requestUrl = if (schemaType == SchemaType.HTTPS) "https://$host$api" else "http://$host$api"
        val requestIpType = when (currentIpType.value) {
            "IPv4" -> RequestIpType.v4
            "IPv6" -> RequestIpType.v6
            "IPv4&IPv6" -> RequestIpType.both
            else -> RequestIpType.auto
        }

        val requestClient = if (requestType == NetRequestType.OKHTTP) OkHttpRequest(getApplication(), requestIpType,
            asyncResolve.value!!
        ) else HttpURLConnectionRequest(getApplication(), requestIpType,
            asyncResolve.value!!)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("httpdns", "before request $requestUrl");
                val response = requestClient.get(requestUrl)
                withContext(Dispatchers.Main) {
                    showDialog?.showRequestResultDialog(response)
                }
            } catch (e: Exception) {
                Log.e("httpdns", Log.getStackTraceString(e))
                withContext(Dispatchers.Main) {
                    showDialog?.showRequestFailedDialog(e)
                }
            }
        }
    }


}