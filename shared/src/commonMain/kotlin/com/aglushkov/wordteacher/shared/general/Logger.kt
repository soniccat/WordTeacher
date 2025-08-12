package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.LoggerConfig
import com.aglushkov.wordteacher.shared.analytics.AnalyticEvent
import com.aglushkov.wordteacher.shared.analytics.Analytics
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

expect class Logger {
    // TODO: consider using a variable instead of a singleton
    companion object {
        var analytics: Analytics?
    }

    fun setupDebug(config: LoggerConfig)
}

fun Logger.Companion.v(message: String, tag: String? = null) {
    co.touchlab.kermit.Logger.v(message, tag = tag.orEmpty())
}

fun Logger.Companion.e(message: String, tag: String? = null) {
    co.touchlab.kermit.Logger.e(message, tag = tag.orEmpty())
}

fun Logger.Companion.setAnalytics(a: Analytics) {
    Logger.analytics = a
}

fun Logger.Companion.exception(message: String, e: Throwable, tag: String? = null) {
    Logger.analytics?.send(AnalyticEvent.createErrorEvent(message, e))
    Logger.e("${e}: ${e.message.orEmpty()}: ${e.stackTraceToString()}", tag)
}

fun <T> Logger.Companion.measure(message: String, block: () -> T): T {
    val time = kotlin.time.Clock.System.now()
    val res = block()
    Logger.v("$message${kotlin.time.Clock.System.now().toEpochMilliseconds() - time.toEpochMilliseconds()}")
    return res
}
