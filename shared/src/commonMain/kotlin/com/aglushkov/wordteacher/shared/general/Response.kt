package com.aglushkov.wordteacher.shared.general

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class Response<out T>

@Serializable
@SerialName("ok")
data class OkResponse<out T>(val value: T) : Response<T>()

@Serializable
data class Error(val message: String)

@Serializable
@SerialName("error")
data class ErrResponse(val value: Error) : Response<Error>()


class ErrorResponseException(val err: Error): Exception(err.message)

fun <T> Response<T>.toOkResult(): T {
    return when(this) {
        is OkResponse -> value
        is ErrResponse -> throw ErrorResponseException(value)
        else -> throw RuntimeException("Unknown response type $this")
    }
}
