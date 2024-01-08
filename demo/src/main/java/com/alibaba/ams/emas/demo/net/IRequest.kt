package com.alibaba.ams.emas.demo.net

import com.alibaba.ams.emas.demo.ui.resolve.Response

/**
 * @author allen.wy
 * @date 2023/5/26
 */
interface IRequest {
    fun get(url: String): Response
}