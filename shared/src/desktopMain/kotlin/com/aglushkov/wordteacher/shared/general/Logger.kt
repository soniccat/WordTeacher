package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.LoggerConfig
import com.aglushkov.wordteacher.shared.analytics.Analytics

actual class Logger {
    actual companion object {
        actual var analytics: Analytics? = null
    }

    actual fun setupDebug(config: LoggerConfig) {
        co.touchlab.kermit.Logger.setMinSeverity(config.minSeverity)
        co.touchlab.kermit.Logger.setLogWriters(config.logWriterList)
    }
}