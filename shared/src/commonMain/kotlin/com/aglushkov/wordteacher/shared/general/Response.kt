package com.aglushkov.wordteacher.shared.general

import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class Response<out T> {

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

fun <T> Response<T>.toOkResponse(): T {
    return when(this) {
        is Response.Ok -> value
        is Response.Err -> throw ErrorResponseException(value, statusCode)
        else -> throw RuntimeException("Unknown response type $this")
    }
}

fun <T> Response<T>.toResource(): Resource<T> {
    return when(this) {
        is Response.Ok -> Resource.Loaded(value)
        is Response.Err -> Resource.Error(ErrorResponseException(value, statusCode))
        else -> throw RuntimeException("Unsupported response type $this")
    }
}

fun <T> Response<T>.setStatusCode(code: Int): Response<T> = when (this) {
    is Response.Ok -> this
    is Response.Err -> Response.Err(value, code) as Response<T>
    else -> throw RuntimeException("Unsupported response type $this")
}

fun Resource<*>?.errorStatusCode(): Int? {
    return when(this) {
        is Resource.Error -> (throwable as? ErrorResponseException)?.statusCode
        else -> null
    }
}
