package com.aglushkov.wordteacher.shared.general

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual class Logger {
    actual companion object {}

    actual fun setupDebug() {
        Napier.base(DebugAntilog())
    }
}