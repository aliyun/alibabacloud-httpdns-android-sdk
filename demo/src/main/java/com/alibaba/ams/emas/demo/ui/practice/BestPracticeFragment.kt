package com.alibaba.ams.emas.demo.ui.practice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.FragmentBestPracticeBinding

/**
 * @author allen.wy
 * @date 2023/6/14
 */
class BestPracticeFragment : Fragment(), IBestPracticeShowDialog {

    private var _binding: FragmentBestPracticeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBestPracticeBinding.inflate(inflater, container, false)

        val viewModel = ViewModelProvider(this)[BestPracticeViewModel::class.java]
        viewModel.showDialog = this

        binding.viewModel = viewModel
        binding.openHttpdnsWebview.setOnClickListener {
            val intent = Intent(activity, HttpDnsWebviewGetActivity::class.java)
            startActivity(intent)
        }

//        binding.openHttpdnsWebviewPost.setOnClickListener {
//            val intent = Intent(activity, HttpDnsWVWebViewActivity::class.java)
//            startActivity(intent)
//        }

        return binding.root
    }

    override fun showResponseDialog(message: String) {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.sni_request)
            setMessage(message)
            setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        }
        builder?.show()
    }

    override fun showNoNetworkDialog() {
        val builder = activity?.let { act -> AlertDialog.Builder(act) }
        builder?.apply {
            setTitle(R.string.tips)
            setMessage(R.string.network_not_connect)
            setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        }
        builder?.show()
    }
}