package com.alibaba.ams.emas.demo.ui.resolve

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alibaba.ams.emas.demo.constant.KEY_RESOLVE_IP_TYPE
import com.alibaba.ams.emas.demo.constant.KEY_RESOLVE_METHOD
import com.alibaba.ams.emas.demo.getAccountPreference
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.FragmentResolveBinding
import org.json.JSONException
import org.json.JSONObject


class ResolveFragment : Fragment(), IResolveShowDialog {

    private var _binding: FragmentResolveBinding? = null

    private val binding get() = _binding!!
    private lateinit var viewModel: ResolveViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ResolveViewModel::class.java]
        viewModel.showDialog = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentResolveBinding.inflate(inflater, container, false)
        viewModel.initData()
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.sdnsParamsInputLayout.visibility = if (viewModel.isSdns.value!!) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.sdnsCacheKeyInputLayout.visibility = if (viewModel.isSdns.value!!) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.enableSdnsResolve.setOnCheckedChangeListener{_, isChecked ->
            viewModel.toggleSdns(isChecked)

            binding.sdnsParamsInputLayout.visibility = if (viewModel.isSdns.value!!) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.sdnsCacheKeyInputLayout.visibility = if (viewModel.isSdns.value!!) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.startResolve.setOnClickListener {
            binding.resolveHostInputLayout.error = ""
            //1. 校验域名是否填写
            val host = binding.resolveHostInputLayout.editText?.text.toString()
            if (TextUtils.isEmpty(host)) {
                binding.resolveHostInputLayout.error = getString(R.string.resolve_host_empty)
                return@setOnClickListener
            }
            var sdnsParams: MutableMap<String, String>? = null
            //2. 校验sdns参数
            if (viewModel.isSdns.value!!) {
                val sdnsParamsStr = binding.sdnsParamsInputLayout.editText?.text.toString()
                if (!TextUtils.isEmpty(sdnsParamsStr)) {
                    try {
                        val sdnsJson = JSONObject(sdnsParamsStr)
                        val keys = sdnsJson.keys()
                        sdnsParams = HashMap()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            sdnsParams[key] = sdnsJson.getString(key)
                        }
                    } catch (e: JSONException) {
                        binding.sdnsParamsInputLayout.error = getString(R.string.input_the_sdns_params_error)
                    }
                }
            }

            var api = binding.requestApiInputLayout.editText?.text.toString()

            val cacheKey = binding.sdnsCacheKeyInputLayout.editText?.text.toString()
            if (!api.startsWith("/")) {
                api = "/$api"
            }
            var index: Int = 0
            do {
                viewModel.startToResolve(host, api, sdnsParams, cacheKey)
                ++index
            } while (index < viewModel.requestNum.value!!)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showSelectResolveIpTypeDialog() {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.select_resolve_ip_type)
            val items = arrayOf("IPv4", "IPv6", "IPv4&IPv6", getString(R.string.auto_get_ip_type))
            val preferences = activity?.let { getAccountPreference(it) }
            val index = when (preferences?.getString(KEY_RESOLVE_IP_TYPE, "IPv4")) {
                "IPv4" -> 0
                "IPv6" -> 1
                "IPv4&IPv6" -> 2
                else -> 3
            }
            var resolvedIpType = "IPv4"
            setSingleChoiceItems(items, index) { _, which ->
                resolvedIpType = when (which) {
                    0 -> "IPv4"
                    1 -> "IPv6"
                    2 -> "IPv4&IPv6"
                    else -> "Auto"
                }
            }
            setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                viewModel.saveResolveIpType(resolvedIpType)
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder?.show()
    }

    override fun showRequestResultDialog(response: Response) {
        val code = response.code
        val body = response.body
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.response_title)
            val message = if (code == 200 && !TextUtils.isEmpty(body)) {
                if (body!!.length <= 100) "$code - $body" else "$code - ${getString(R.string.body_large_see_log)}"
            } else {
                code.toString()
            }
            setMessage(message)
            setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        }
        builder?.show()
    }

    override fun showRequestFailedDialog(e: Throwable) {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.response_title)
            setMessage(getString(R.string.request_exception, e.message))
            setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        }
        builder?.show()
    }

    override fun showResolveMethodDialog() {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.select_resolve_method)
            val items = arrayOf("同步方法", "异步方法", "同步非阻塞方法")
            val preferences = activity?.let { getAccountPreference(it) }

            var resolvedMethod = preferences?.getString(KEY_RESOLVE_METHOD, "getHttpDnsResultForHostSync(String host, RequestIpType type)").toString()
            val index = when (resolvedMethod) {
                "getHttpDnsResultForHostSync(String host, RequestIpType type)" -> 0
                "getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback)" -> 1
                "getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type)" -> 2
                else -> 3
            }
            setSingleChoiceItems(items, index) { _, which ->
                resolvedMethod = when (which) {
                    0 -> "getHttpDnsResultForHostSync(String host, RequestIpType type)"
                    1 -> "getHttpDnsResultForHostAsync(String host, RequestIpType type, HttpDnsCallback callback)"
                    2 -> "getHttpDnsResultForHostSyncNonBlocking(String host, RequestIpType type)"
                    else -> "getHttpDnsResultForHostSync(String host, RequestIpType type)"
                }
            }

            setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                viewModel.saveResolveMethod(resolvedMethod)
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder?.show()
    }

    override fun showRequestNumberDialog() {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.select_request_num)
            val items = arrayOf("1", "2", "3", "4", "5")

            val index = viewModel.requestNum.value!! - 1
            var num = viewModel.requestNum.value
            setSingleChoiceItems(items, index) { _, which ->
                num = which + 1
            }

            setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                viewModel.saveRequestNumber(num!!)
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder?.show()
    }
}