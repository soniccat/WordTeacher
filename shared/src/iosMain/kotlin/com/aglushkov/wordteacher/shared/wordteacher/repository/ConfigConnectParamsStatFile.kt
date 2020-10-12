package com.aglushkov.wordteacher.shared.wordteacher.repository

import com.aglushkov.wordteacher.shared.wordteacher.extensions.documentDirectoryFilePath
import com.aglushkov.wordteacher.shared.wordteacher.extensions.toByteArray
import com.aglushkov.wordteacher.shared.wordteacher.extensions.toNSData
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pointed
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual class ConfigConnectParamsStatFile {
    private val path = NSFileManager.documentDirectoryFilePath(fileName())

    actual fun loadContent(): ByteArray {
        val data: NSData? = NSData.dataWithContentsOfFile(path)

        return data?.let { data ->
            data.toByteArray()
        } ?: run {
            ByteArray(0)
        }
    }

    actual fun saveContent(bytes: ByteArray) {
        bytes.toNSData()?.writeToFile(path, true)
    }
}