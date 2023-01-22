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

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "status"
        serializersModule = SerializersModule {
            polymorphic(Response::class) {
                subclass(OkResponse.serializer(AuthData.serializer()))
                subclass(ErrResponse.serializer())
            }
        }
    }

    suspend fun auth(network: NetworkType, token: String): Response<AuthData> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
                this.setBody(json.encodeToString(AuthInput(token)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            json.decodeFromString(stringResponse)
        }
    }

    suspend fun refresh(token: AuthToken): Response<AuthToken> {
        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/auth/refresh") {
                this.setBody(json.encodeToString(RefreshInput(token.accessToken, token.refreshToken)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            json.decodeFromString(stringResponse)
        }
    }
}

private const val TAG = "SpaceAuth"
