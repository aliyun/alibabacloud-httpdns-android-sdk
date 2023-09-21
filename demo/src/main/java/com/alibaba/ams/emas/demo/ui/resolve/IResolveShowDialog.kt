package com.alibaba.ams.emas.demo.ui.resolve

interface IResolveShowDialog {
    fun showSelectResolveIpTypeDialog()

    fun showRequestResultDialog(response: Response)

    fun showRequestFailedDialog(e: Throwable)
}