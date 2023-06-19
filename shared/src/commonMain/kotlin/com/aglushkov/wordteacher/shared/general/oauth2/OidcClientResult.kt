package com.aglushkov.wordteacher.shared.general.oauth2

/**
 * Describes the result from the OidcClient.
 */
sealed class OidcClientResult<T> {
    /** An error result. */
    class Error<T> constructor(
        /** The exception associated with the error. */
        val exception: Exception,
    ) : OidcClientResult<T>() {
        /**
         * The response type used to represent a completed HTTP response, but a non successful status code.
         */
        class HttpResponseException internal constructor(
            /** The HTTP response code associated with the error. */
            val responseCode: Int,
            /** The error returned by the Authorization Server. */
            val error: String?,
            /** The error description returned by the Authorization Server. */
            val errorDescription: String?,
        ) : Exception(errorDescription ?: error ?: "HTTP Error: status code - $responseCode")

        /**
         * The response failed due to no [OidcEndpoints].
         *
         * This can happen due to a misconfigured setup, or just a common HTTP error.
         */
        class OidcEndpointsNotAvailableException internal constructor() : Exception("OIDC Endpoints not available.")
    }

    /** Success with the expected result. */
    class Success<T> constructor(
        /** The result of the success result. */
        val result: T,
    ) : OidcClientResult<T>()

    /**
     * Returns the encapsulated value if this instance represents [Success] or throws the encapsulated [Exception] if it is [Error].
     */
    fun getOrThrow(): T {
        when (this) {
            is Error -> {
                throw exception
            }
            is Success -> {
                return result
            }
        }
    }
}
