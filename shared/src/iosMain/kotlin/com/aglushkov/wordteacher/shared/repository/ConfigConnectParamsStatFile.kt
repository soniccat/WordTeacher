package com.aglushkov.wordteacher.shared.repository

import com.aglushkov.wordteacher.shared.extensions.documentDirectoryFilePath
import com.aglushkov.wordteacher.shared.extensions.toByteArray
import com.aglushkov.wordteacher.shared.extensions.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

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