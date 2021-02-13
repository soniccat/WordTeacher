package com.aglushkov.wordteacher.shared.general

import com.github.aakira.napier.Napier
import kotlinx.datetime.Clock

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

fun Logger.Companion.measure(message: String, block: () -> Unit) {
    val time = Clock.System.now()
    block()
    Logger.v("$message${Clock.System.now().toEpochMilliseconds() - time.toEpochMilliseconds()}")
}