package com.alibaba.ams.emas.demo.ui.practice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.ActivityHttpDnsWebviewBinding
import com.aliyun.ams.httpdns.demo.databinding.ActivityHttpDnsWvwebViewBinding

class HttpDnsWVWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHttpDnsWvwebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_http_dns_wvweb_view)

        binding = ActivityHttpDnsWvwebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.wvWebviewToolbar.title = getString(R.string.httpdns_webview_post_best_practice)
        setSupportActionBar(binding.wvWebviewToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//添加默认的返回图标
        supportActionBar?.setHomeButtonEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}