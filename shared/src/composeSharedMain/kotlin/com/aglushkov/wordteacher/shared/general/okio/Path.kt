package com.aglushkov.wordteacher.shared.general.okio

import okio.Path

actual fun Path.deleteIfExists(): Boolean {
    val f = toFile()
    return if (f.exists()) {
        f.delete()
    } else {
        true
    }
}

actual fun Path.useAsTmp(block: (Path)->Unit): Boolean {
    if (!deleteIfExists()) {
        return false
    }
    block(this)
    deleteIfExists()

    return true
}