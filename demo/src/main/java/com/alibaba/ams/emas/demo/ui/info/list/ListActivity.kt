package com.alibaba.ams.emas.demo.ui.info.list

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.ActivityListBinding

class ListActivity : AppCompatActivity(), ListAdapter.OnDeleteListener {

    private lateinit var binding: ActivityListBinding

    private val infoList: MutableList<ListItem> = mutableListOf()
    private lateinit var listAdapter: ListAdapter
    private var listType: Int = kListItemTypeIPRanking

    private lateinit var viewModel: ListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var title = ""
        intent?.let {
            listType = intent.getIntExtra("list_type", kListItemTypeIPRanking)
            title = when (listType) {
                kListItemTypeCacheTtl -> getString(R.string.ttl_cache_list)
                kListItemTypeHostWithFixedIP -> getString(R.string.host_fixed_ip_list)
                kListItemPreResolve -> getString(R.string.pre_resolve_list)
                kListItemTypeBlackList -> getString(R.string.host_black_list)
                kListItemBatchResolve -> getString(R.string.batch_resolve_list)
                kListItemTag -> getString(R.string.add_tag)
                else -> getString(R.string.ip_probe_list)
            }
        }
        viewModel = ViewModelProvider(this)[ListViewModel::class.java]

        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.infoListToolbar.title = title
        setSupportActionBar(binding.infoListToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//添加默认的返回图标
        supportActionBar?.setHomeButtonEnabled(true)

        binding.infoListView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        viewModel.initData(listType, infoList)

        listAdapter = ListAdapter(this, infoList, this)
        binding.infoListView.adapter = listAdapter

        binding.fab.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        when (listType) {
            kListItemTag -> {
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
                val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
                editText.hint = getString(R.string.add_tag_hint)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_tag))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = editText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.host_fixed_ip_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                viewModel.toAddTag(host, listAdapter)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            kListItemTypeHostWithFixedIP -> {
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
                val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
                editText.hint = getString(R.string.add_host_fixed_ip_hint)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_host_fixed_ip))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = editText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.host_fixed_ip_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                viewModel.toAddHostWithFixedIP(host, listAdapter)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            kListItemTypeBlackList -> {
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input, null)
                val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
                editText.hint = getString(R.string.add_host_to_black_list_hint)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_host_to_black_list))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = editText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.host_to_black_list_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                viewModel.toAddHostInBlackList(host, listAdapter)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            kListItemPreResolve -> {
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input_3, null)
                val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
                editText.hint = getString(R.string.add_pre_resolve_hint)
                val ipTypeGroup = input.findViewById<RadioGroup>(R.id.ip_type)

                var view = createIpTypeRadio(this)
                view.text = "IPv4"
                view.isChecked = true
                view.tag = 0
                ipTypeGroup.addView(view)

                view = createIpTypeRadio(this)
                view.text = "IPv6"
                view.tag = 1
                ipTypeGroup.addView(view)

                view = createIpTypeRadio(this)
                view.text = "IPv4&IPv6"
                view.tag = 2
                ipTypeGroup.addView(view)

                view = createIpTypeRadio(this)
                view.text = "自动判断IP类型"
                view.tag = 3
                ipTypeGroup.addView(view)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_pre_resolve))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = editText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.pre_resolve_host_is_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                viewModel.toAddPreResolveHost(host, listAdapter, ipTypeGroup.findViewById<RadioButton>(ipTypeGroup.checkedRadioButtonId).tag as Int)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            kListItemBatchResolve -> {
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input_3, null)
                val editText = input.findViewById<AppCompatEditText>(R.id.add_input)
                editText.hint = getString(R.string.add_batch_resolve_hint)
                val ipTypeGroup = input.findViewById<RadioGroup>(R.id.ip_type)
                var view = createIpTypeRadio(this)
                view.text = "IPv4"
                view.isChecked = true
                view.tag = 0
                ipTypeGroup.addView(view)
                view = createIpTypeRadio(this)
                view.text = "IPv6"
                view.tag = 1
                ipTypeGroup.addView(view)
                view = createIpTypeRadio(this)
                view.text = "IPv4&IPv6"
                view.tag = 2
                ipTypeGroup.addView(view)
                view = createIpTypeRadio(this)
                view.text = "自动判断IP类型"
                view.tag = 3
                ipTypeGroup.addView(view)
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_batch_resolve))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = editText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.batch_resolve_host_is_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                viewModel.toAddBatchResolveHost(host, listAdapter, ipTypeGroup.findViewById<RadioButton>(ipTypeGroup.checkedRadioButtonId).tag as Int)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            else -> {
                val isTtl = listType == kListItemTypeCacheTtl
                val input = LayoutInflater.from(this).inflate(R.layout.dialog_input_2, null)
                val hostEditText = input.findViewById<AppCompatEditText>(R.id.input_content_1)
                val intEditText = input.findViewById<AppCompatEditText>(R.id.input_content_2)
                intEditText.inputType = EditorInfo.TYPE_CLASS_NUMBER

                hostEditText.hint =
                    getString(if (isTtl) R.string.add_ttl_host_hint else R.string.add_ip_probe_host_hint)
                intEditText.hint =
                    getString(if (isTtl) R.string.add_ttl_ttl_hint else R.string.add_ip_probe_port_hint)

                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(if (isTtl) R.string.add_custom_ttl else R.string.add_ip_probe))
                    .setView(input)
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        when (val host = hostEditText.text.toString()) {
                            "" -> Toast.makeText(
                                this@ListActivity,
                                R.string.host_is_empty,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                when (val intValue = intEditText.text.toString()) {
                                    "" -> Toast.makeText(
                                        this@ListActivity,
                                        if (isTtl) R.string.ttl_is_empty else R.string.port_is_empty,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    else -> {
                                        try {
                                            if (isTtl) {
                                                viewModel.toSaveTtlCache(
                                                    host,
                                                    intValue.toInt(),
                                                    listAdapter
                                                )
                                            } else {
                                                viewModel.toSaveIPProbe(
                                                    host,
                                                    intValue.toInt(),
                                                    listAdapter
                                                )
                                            }
                                        } catch (e: NumberFormatException) {
                                            Toast.makeText(
                                                this@ListActivity,
                                                R.string.ttl_is_not_number,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    fun createIpTypeRadio(context: Context): RadioButton {
        val btn = RadioButton(context)
        btn.id = View.generateViewId()
        val params = RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT)
        btn.layoutParams = params

        return btn
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTagDeleted(position: Int) {
        viewModel.onTagDeleted(position)
    }

    override fun onHostWithFixedIPDeleted(position: Int) {
        //只能重启生效
        viewModel.onHostWithFixedIPDeleted(position)
    }

    override fun onIPRankingItemDeleted(position: Int) {
        viewModel.onIPProbeItemDeleted(position)
    }

    override fun onTtlDeleted(host: String) {
        viewModel.onTtlDeleted(host)
    }

    override fun onPreResolveDeleted(host: String, intValue: Int) {
        Log.d("httpdns", "onPreResolveDeleted")
        viewModel.onPreResolveDeleted(host, intValue)
    }

    override fun onHostBlackListDeleted(position: Int) {
        viewModel.onHostBlackListDeleted(position)
    }

    override fun onBatchResolveDeleted(host: String, intValue: Int) {
        viewModel.onBatchResolveDeleted(host, intValue)
    }
}