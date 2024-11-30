package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
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
const val HeaderAppVersion = "X-App-Version"

@Serializable
data class SpaceAuthData(
    @SerialName("token") val authToken: SpaceAuthToken,
    @SerialName("user") val user: SpaceAuthUser
)

@Serializable
data class SpaceAuthToken(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class SpaceAuthUser(
    @SerialName("id") val id: String,
    @SerialName("networkType") val networkType: SpaceAuthService.NetworkType,
)

class SpaceAuthService(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val secureCodec: SecureCodec,
) {
    @Serializable
    enum class NetworkType(val value: String) {
        @SerialName("google")
        Google("google"),
        @SerialName("vkid")
        VKID("vkid"),
        @SerialName("yandexid")
        YandexId("yandexid");

        override fun toString(): String = when(this) {
            Google -> "Google"
            YandexId -> "Yandex ID"
            VKID -> "VK ID"
        }
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
                    subclass(Response.Ok.serializer(SpaceAuthData.serializer()))
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
                    subclass(Response.Ok.serializer(SpaceAuthToken.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun auth(network: NetworkType, token: String): Response<SpaceAuthData> {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse =
                httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
                    contentType(ContentType.Application.Json)
                    setBody(authJson.encodeToString(AuthInput(token)))
                }
            val stringResponse: String = res.body()
            authJson.decodeFromString<Response<SpaceAuthData>>(stringResponse)
                .mapOkData {
                    it.copy(
                        authToken = it.authToken.encrypt(secureCodec),
                    )
                }
                .setStatusCode(res.status.value)
        }
    }

    suspend fun refresh(token: SpaceAuthToken): Response<SpaceAuthToken> {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse =
                httpClient.post(urlString = "${baseUrl}/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        refreshJson.encodeToString(
                            RefreshInput(
                                secureCodec.decrypt(token.accessToken.decodeBase64Bytes())!!.decodeToString(),
                                secureCodec.decrypt(token.refreshToken.decodeBase64Bytes())!!.decodeToString(),
                            )
                        )
                    )
                }
            val stringResponse: String = res.body()
            refreshJson.decodeFromString<Response<SpaceAuthToken>>(stringResponse)
                .mapOkData {
                   it.encrypt(secureCodec)
                }
                .setStatusCode(res.status.value)
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

private fun SpaceAuthToken.encrypt(secureCodec: SecureCodec) =
    copy(
        accessToken = secureCodec.encrypt(
            accessToken.toByteArray()
        ).encodeBase64(),
        refreshToken = secureCodec.encrypt(
            refreshToken.toByteArray()
        ).encodeBase64(),
    )
