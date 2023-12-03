package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.LoggerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual class Logger {
    actual companion object {}

    actual fun setupDebug(config: LoggerConfig) {
        co.touchlab.kermit.Logger.setMinSeverity(config.minSeverity)
        co.touchlab.kermit.Logger.setLogWriters(config.logWriterList)
    }
}