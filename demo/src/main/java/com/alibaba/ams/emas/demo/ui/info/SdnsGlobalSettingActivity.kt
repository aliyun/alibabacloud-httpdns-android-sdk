package com.alibaba.ams.emas.demo.ui.info

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.ams.emas.demo.constant.KEY_SDNS_GLOBAL_PARAMS
import com.alibaba.ams.emas.demo.getAccountPreference
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.ActivitySdnsGlobalSettingBinding
import org.json.JSONException
import org.json.JSONObject

class SdnsGlobalSettingActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySdnsGlobalSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getAccountPreference(this)

        binding = ActivitySdnsGlobalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(R.string.input_the_sdns_params)

        val params = preferences.getString(KEY_SDNS_GLOBAL_PARAMS, "")
        binding.sdnsParamsInputLayout.editText?.setText(params)
        binding.toolbar.setNavigationOnClickListener {
            val sdnsParamsStr = binding.sdnsParamsInputLayout.editText?.text.toString()
            if (!TextUtils.isEmpty(sdnsParamsStr)) {
                try {
                    val sdnsJson = JSONObject(sdnsParamsStr)
                    preferences.edit().putString(KEY_SDNS_GLOBAL_PARAMS, sdnsParamsStr).apply()
                    onBackPressed()
                } catch (e: JSONException) {
                    binding.sdnsParamsInputLayout.error = getString(R.string.input_the_sdns_params_error)
                }
            } else {
                preferences.edit().putString(KEY_SDNS_GLOBAL_PARAMS, "").apply()
                onBackPressed()
            }
        }
    }


}