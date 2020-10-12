package com.aglushkov.wordteacher.shared.wordteacher.extensions

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.create

fun ByteArray.toNSData(): NSData? = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong())
}