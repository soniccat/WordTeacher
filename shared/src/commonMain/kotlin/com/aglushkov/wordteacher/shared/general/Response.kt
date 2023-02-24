package com.aglushkov.wordteacher.shared.general

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Response<out T> {

    @Serializable
    @SerialName("ok")
    data class Ok<out T>(val value: T) : Response<T>()

    @Serializable
    @SerialName("error")
    data class Err(val value: Error, val statusCode: Int = 0) : Response<Error>()
}

@Serializable
data class Error(val message: String)

class ErrorResponseException(val err: Error, val statusCode: Int): Exception(err.message)

fun <T> Response<T>.toOkResult(): T {
    return when(this) {
        is Response.Ok -> value
        is Response.Err -> throw ErrorResponseException(value, statusCode)
        else -> throw RuntimeException("Unknown response type $this")
    }
}

fun <T> Response<T>.setStatusCode(code: Int): Response<T> = when (this) {
    is Response.Ok -> this
    is Response.Err -> Response.Err(value, statusCode) as Response<T>
}
