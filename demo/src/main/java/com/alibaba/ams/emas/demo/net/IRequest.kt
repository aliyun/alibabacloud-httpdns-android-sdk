package com.alibaba.ams.emas.demo.net

import com.alibaba.ams.emas.demo.ui.resolve.Response

interface IRequest {
    fun get(url: String): Response
}