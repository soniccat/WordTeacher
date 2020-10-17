package com.aglushkov.wordteacher.shared.general

import com.github.aakira.napier.DebugAntilog
import com.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual class Logger {
    actual companion object {}

    private val defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    actual fun setupDebug() {
        Napier.base(DebugAntilog())

        defaultScope.launch {
            // Have to initialize it for another thread too because Napier uses @ThreadLocal
            Napier.base(DebugAntilog())
        }
    }
}