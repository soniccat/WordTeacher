package com.aglushkov.wordteacher.repository

import com.aglushkov.wordteacher.shared.repository.Config
import io.ktor.utils.io.core.toByteArray
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


data class ConfigConnectParamsStat (
    @SerialName("type") val type: Config.Type,
    @SerialName("connectParamsHash") val connectParamsHash: Int,
    @SerialName("errorDate") var errorDate: Instant,
    @SerialName("nextTryDate") var nextTryDate: Instant
) {
    companion object
}

fun ConfigConnectParamsStat.Companion.fromByteArray(byteArray: ByteArray): List<ConfigConnectParamsStat> {
    val text = byteArray.decodeToString() // TODO: decipher
    return Json.decodeFromString(text)
}

fun List<ConfigConnectParamsStat>.toByteArray(): ByteArray {
    return Json.encodeToString(this).toByteArray() // TODO: cipher
}
