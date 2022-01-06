package com.aglushkov.wordteacher.shared.general

interface SimpleRouter {
    val isDialog: Boolean
        get() = false

    fun onScreenFinished(inner: Any, result: Result)

    data class Result(val isCancelled: Boolean, val data: Any? = null)
}