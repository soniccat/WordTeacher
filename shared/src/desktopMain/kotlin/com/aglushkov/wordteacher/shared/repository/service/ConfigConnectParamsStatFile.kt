package com.aglushkov.wordteacher.shared.repository.service

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

actual class ConfigConnectParamsStatFile {
    private val userPath = /*FileSystems.getDefault().getPath(".").toAbsolutePath().toString();*/ System.getProperty("user.dir")

    actual fun loadContent(): ByteArray {
        val file = File(userPath, fileName)
        return file.readBytes()
    }

    actual fun saveContent(bytes: ByteArray) {
        val file = File(userPath, fileName)
        file.writeBytes(bytes)
    }
}