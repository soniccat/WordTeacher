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
    data class Input(
        @SerialName("token") val token: String,
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
        Logger.v("Loading", tag = TAG)

        val res: HttpResponse =
            httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
                this.setBody(json.encodeToString(Input(token)))
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            logResponse(res, stringResponse)

            json.decodeFromString(stringResponse)
        }
    }

    private fun logResponse(
        response: HttpResponse,
        stringResponse: String
    ) {
        if (response.status == HttpStatusCode.OK) {
            Logger.v("Loaded", tag = TAG)
        } else {
            Logger.e("Status: ${response.status} response: $stringResponse", tag = TAG)
        }
    }
}

private const val TAG = "SpaceAuth"
