package com.aglushkov.wordteacher.shared.wordteacher.repository

import android.content.Context

actual class ConfigConnectParamsStatFile(
    private val context: Context
) {
    actual fun loadContent(): ByteArray {
        return context.openFileInput(fileName()).use {
            it.readBytes()
        }
    }

    actual fun saveContent(bytes: ByteArray) {
        context.openFileOutput(fileName(), Context.MODE_PRIVATE).use {
            it.write(bytes)
        }
    }
}