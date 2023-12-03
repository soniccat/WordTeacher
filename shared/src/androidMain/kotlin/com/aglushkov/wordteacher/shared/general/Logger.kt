package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.LoggerConfig

actual class Logger {
    actual companion object {}

    actual fun setupDebug(config: LoggerConfig) {
        co.touchlab.kermit.Logger.setMinSeverity(config.minSeverity)
        co.touchlab.kermit.Logger.setLogWriters(config.logWriterList)
    }
}