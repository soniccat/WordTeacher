package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

const val CookieSession = "session"
const val HeaderDeviceId = "X-Device-Id"
const val HeaderDeviceType = "X-Device-Type"
const val HeaderAccessToken = "X-Access-Token"

@Serializable
data class AuthData(
    @SerialName("token") val authToken: AuthToken,
    @SerialName("user") val user: AuthUser
)

@Serializable
data class AuthToken(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class AuthUser(
    @SerialName("id") val id: String,
    @SerialName("networkType") val networkType: SpaceAuthService.NetworkType?,
)

class SpaceAuthService(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    @Serializable
    enum class NetworkType(val value: String) {
        @SerialName("google")
        Google("google"),
    }

    @Serializable
    data class AuthInput(
        @SerialName("token") val token: String,
    )

    @Serializable
    data class RefreshInput(
        @SerialName("accessToken") val accessToken: String,
        @SerialName("refreshToken") val refreshToken: String,
    )

    private val authJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(AuthData.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    private val refreshJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(AuthToken.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun auth(network: NetworkType, token: String): Response<AuthData> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
                this.setBody(authJson.encodeToString(AuthInput(token)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            authJson.decodeFromString<Response<AuthData>>(stringResponse).setStatusCode(res.status.value)
        }
    }

    suspend fun refresh(token: AuthToken): Response<AuthToken> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/auth/refresh") {
                this.setBody(refreshJson.encodeToString(RefreshInput(token.accessToken, token.refreshToken)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            refreshJson.decodeFromString<Response<AuthToken>>(stringResponse).setStatusCode(res.status.value)
        }
    }

    fun isAuthUrl(url: Url): Boolean {
        return url.isAuthSocialUrl() || url.isAuthRefreshUrl()
    }

    private fun Url.isAuthRefreshUrl(): Boolean {
        return pathSegments.size >= 2 && pathSegments.subList(pathSegments.size - 2, pathSegments.size) == listOf("auth", "refresh")
    }

    private fun Url.isAuthSocialUrl(): Boolean {
        return pathSegments.size >= 3 && pathSegments.subList(pathSegments.size - 3, pathSegments.size - 1) == listOf("auth", "social")
    }
}
