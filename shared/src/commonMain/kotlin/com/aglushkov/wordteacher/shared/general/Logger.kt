package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.LoggerConfig
import kotlinx.datetime.Clock

expect class Logger {
    // TODO: consider using a variable instead of a singleton
    companion object {}

    fun setupDebug(config: LoggerConfig)
}

fun Logger.Companion.v(message: String, tag: String? = null) {
    co.touchlab.kermit.Logger.v(message, tag = tag.orEmpty())
}

fun Logger.Companion.e(message: String, tag: String? = null) {
    co.touchlab.kermit.Logger.e(message, tag = tag.orEmpty())
}

fun Logger.Companion.exception(e: Throwable, tag: String? = null) {
    Logger.e("${e}: ${e.message.orEmpty()}: ${e.stackTraceToString()}", tag)
}

fun <T> Logger.Companion.measure(message: String, block: () -> T): T {
    val time = Clock.System.now()
    val res = block()
    Logger.v("$message${Clock.System.now().toEpochMilliseconds() - time.toEpochMilliseconds()}")
    return res
}
