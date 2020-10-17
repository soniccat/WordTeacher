package com.aglushkov.wordteacher.shared.general

import com.github.aakira.napier.Napier

expect class Logger {
    companion object {}

    fun setupDebug()
}

fun Logger.Companion.v(message: String, tag: String? = null) {
    Napier.v(message, tag = tag)
}

fun Logger.Companion.e(message: String, tag: String? = null) {
    Napier.e(message, tag = tag)
}