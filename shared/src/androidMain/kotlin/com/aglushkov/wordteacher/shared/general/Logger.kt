package com.aglushkov.wordteacher.shared.general

import com.github.aakira.napier.DebugAntilog
import com.github.aakira.napier.Napier

actual class Logger {
    actual companion object {}

    actual fun setupDebug() {
        Napier.base(DebugAntilog())
    }
}