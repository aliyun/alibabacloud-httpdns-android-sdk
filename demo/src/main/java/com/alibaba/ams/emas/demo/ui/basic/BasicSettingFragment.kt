package com.alibaba.ams.emas.demo.ui.basic

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alibaba.ams.emas.demo.constant.KEY_REGION
import com.alibaba.ams.emas.demo.getAccountPreference
import com.alibaba.ams.emas.demo.ui.info.list.ListActivity
import com.alibaba.ams.emas.demo.ui.info.list.kListItemTag
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.FragmentBasicSettingBinding

class BasicSettingFragment : Fragment(), IBasicShowDialog {

    private var _binding: FragmentBasicSettingBinding? = null

    private val binding get() = _binding!!
    private lateinit var viewModel: BasicSettingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(this,)[BasicSettingViewModel::class.java]
        viewModel.showDialog = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBasicSettingBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        val root: View = binding.root
        viewModel.initData()

        binding.viewModel = viewModel
        binding.jumpToAddTag.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemTag)
            startActivity(intent)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showSelectRegionDialog() {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.select_region)
            val china = getString(R.string.china)
            val chinaHK = getString(R.string.china_hk)
            val singapore = getString(R.string.singapore)
            val germany = getString(R.string.germany)
            val america = getString(R.string.america)
            val pre = getString(R.string.pre)
            val items = arrayOf(china, chinaHK, singapore, germany, america, pre)
            var region = ""
            val preferences = activity?.let { getAccountPreference(it) }
            val index = when (preferences?.getString(KEY_REGION, "cn")) {
                "hk" -> 1
                "sg" -> 2
                "de" -> 3
                "us" -> 4
                "pre" -> 5
                else -> 0
            }
            setSingleChoiceItems(items, index) { _, which ->
                region = when (which) {
                    1 -> "hk"
                    2 -> "sg"
                    3 -> "de"
                    4 -> "us"
                    5 -> "pre"
                    else -> "cn"
                }
            }
            setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                viewModel.saveRegion(region)
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder?.show()
    }

    override fun showSetTimeoutDialog() {
        val input = LayoutInflater.from(activity).inflate(R.layout.dialog_input, null)
        val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
        editText.hint = getString(R.string.timeout_hint)
        editText.inputType = EditorInfo.TYPE_CLASS_NUMBER

        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(getString(R.string.set_timeout))
            setView(input)
            setPositiveButton(R.string.confirm) { dialog, _ ->
                when (val timeout = editText.text.toString()) {
                    "" -> Toast.makeText(activity, R.string.timeout_empty, Toast.LENGTH_SHORT)
                        .show()
                    else -> viewModel.saveTimeout(timeout.toInt())
                }
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }

    }

    override fun showInputHostDialog() {
        val input = LayoutInflater.from(activity).inflate(R.layout.dialog_input, null)
        val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
        editText.hint = getString(R.string.clear_cache_hint)

        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(getString(R.string.clear_host_cache))
            setView(input)
            setPositiveButton(R.string.confirm) { dialog, _ ->
                viewModel.clearDnsCache(editText.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    override fun showAddPreResolveDialog() {
        val input = LayoutInflater.from(activity).inflate(R.layout.dialog_input, null)
        val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
        editText.hint = getString(R.string.add_pre_resolve_hint)

        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(getString(R.string.add_pre_resolve))
            setView(input)
            setPositiveButton(R.string.confirm) { dialog, _ ->
                when (val host = editText.text.toString()) {
                    "" -> Toast.makeText(activity, R.string.pre_resolve_host_is_empty, Toast.LENGTH_SHORT)
                        .show()
                    else -> viewModel.addPreResolveDomain(host)
                }
                dialog.dismiss()
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    override fun onHttpDnsInit() {
        activity?.runOnUiThread(Runnable {
            _binding?.initHttpdns?.setText(R.string.inited_httpdns)
            _binding?.initHttpdns?.isClickable = false
        })
    }
}