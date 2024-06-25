package com.alibaba.ams.emas.demo.ui.info

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alibaba.ams.emas.demo.ui.info.list.*
import com.aliyun.ams.httpdns.demo.BuildConfig
import com.aliyun.ams.httpdns.demo.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null

    private val binding get() = _binding!!

    private lateinit var viewModel: InfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[InfoViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.initData()

        binding.infoPkgName.text = activity?.packageName
        binding.infoSecretView.apply {
            visibility = if (TextUtils.isEmpty(BuildConfig.SECRET_KEY))  View.GONE else View.VISIBLE
        }

        binding.jumpToPreResolve.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemPreResolve)
            startActivity(intent)
        }

        binding.jumpToIpRanking.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemTypeIPRanking)
            startActivity(intent)
        }

        binding.jumpToHostFiexIp.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemTypeHostWithFixedIP)
            startActivity(intent)
        }

        binding.jumpToHostBlackList.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemTypeBlackList)
            startActivity(intent)
        }

        binding.jumpToTtlCache.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemTypeCacheTtl)
            startActivity(intent)
        }

        binding.jumpToSdnsGlobalParams.setOnClickListener {
            val intent = Intent(activity, SdnsGlobalSettingActivity::class.java)
            startActivity(intent)
        }

        binding.jumpToBatchResolve.setOnClickListener {
            val intent = Intent(activity, ListActivity::class.java)
            intent.putExtra("list_type", kListItemBatchResolve)
            startActivity(intent)
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}