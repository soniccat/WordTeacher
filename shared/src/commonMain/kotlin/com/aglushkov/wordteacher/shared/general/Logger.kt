package com.aglushkov.wordteacher.shared.general

import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock

expect class Logger {
    // TODO: consider using a variable instead of a singleton
    companion object {}

    fun setupDebug()
}

fun Logger.Companion.v(message: String, tag: String? = null) {
    Napier.v(message, tag = tag)
}

fun Logger.Companion.e(message: String, tag: String? = null) {
    Napier.e(message, tag = tag)
}

fun Logger.Companion.exception(e: Throwable, tag: String? = null) {
    Logger.e("${e}: ${e.message.orEmpty()}: ${e.stackTraceToString()}", tag)
}

fun Logger.Companion.measure(message: String, block: () -> Unit) {
    val time = Clock.System.now()
    block()
    Logger.v("$message${Clock.System.now().toEpochMilliseconds() - time.toEpochMilliseconds()}")
}