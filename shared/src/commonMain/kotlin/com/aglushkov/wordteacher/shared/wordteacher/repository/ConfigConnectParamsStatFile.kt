package com.aglushkov.wordteacher.shared.wordteacher.repository

expect class ConfigConnectParamsStatFile {
    fun loadContent(): ByteArray
    fun saveContent(bytes: ByteArray)
}

fun ConfigConnectParamsStatFile.fileName(): String {
    return "config_connect_params_stats"
}