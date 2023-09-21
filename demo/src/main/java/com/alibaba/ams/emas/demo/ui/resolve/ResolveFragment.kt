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
import com.alibaba.ams.emas.demo.getAccountPreference
import com.alibaba.sdk.android.httpdns.utils.CommonUtil
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.FragmentResolveBinding


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

        binding.startResolve.setOnClickListener {
            binding.resolveHostInputLayout.error = ""
            //1. 校验域名是否填写
            val host = binding.resolveHostInputLayout.editText?.text.toString()
            if (TextUtils.isEmpty(host)) {
                binding.resolveHostInputLayout.error = getString(R.string.resolve_host_empty)
                return@setOnClickListener
            }
            //2. 域名是否正确
            if (!CommonUtil.isAHost(host)) {
                binding.resolveHostInputLayout.error = getString(R.string.host_illegal)
                return@setOnClickListener
            }
            //3. 域名不能是ip
            if (CommonUtil.isAnIP(host)) {
                binding.resolveHostInputLayout.error = getString(R.string.host_is_ip)
                return@setOnClickListener
            }

            var api = binding.requestApiInputLayout.editText?.text.toString()
            if (!api.startsWith("/")) {
                api = "/$api"
            }
            viewModel.startToResolve(host, api)
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
}