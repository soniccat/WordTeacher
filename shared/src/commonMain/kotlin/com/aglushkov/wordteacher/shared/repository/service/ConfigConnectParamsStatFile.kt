package com.aglushkov.wordteacher.shared.repository.service

// TODO: come up with a more general solution for any file like MPFile or change with square okio
expect class ConfigConnectParamsStatFile {
    fun loadContent(): ByteArray
    fun saveContent(bytes: ByteArray)
}

val ConfigConnectParamsStatFile.fileName: String
    get(): String = "config_connect_params_stats"