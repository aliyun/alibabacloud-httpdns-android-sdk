package com.alibaba.ams.emas.demo.ui.resolve

/**
 * @author allen.wy
 * @date 2023/5/26
 */
interface IResolveShowDialog {
    fun showSelectResolveIpTypeDialog()

    fun showRequestResultDialog(response: Response)

    fun showRequestFailedDialog(e: Throwable)

    fun showResolveMethodDialog()

    fun showRequestNumberDialog()
}