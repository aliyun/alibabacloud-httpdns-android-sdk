package com.alibaba.ams.emas.demo.net

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

class OkHttpLog: HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Log.d("Okhttp", message)
    }
}