package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
)

class SpaceAuthService(
    private val baseUrl: String,
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo
) {
    companion object {}

    @Serializable
    enum class NetworkType(val value: String) {
        @SerialName("google") Google("google"),
    }

    @Serializable
    data class Input(
        @SerialName("token") val token: String,
    )

    private val httpClient = HttpClient() {
        install(
            createClientPlugin("SpacePlugin") {
                onRequest { request, content ->
                    request.headers {
                        set("deviceType", "android")
                        set("deviceId", deviceIdRepository.deviceId())
                        set(HttpHeaders.UserAgent, appInfo.getUserAgent())
                    }
                }
            }
        )
    }/*.also {
        it.plugin(HttpSend).intercept { request ->
            execute(request)
        }
    }*/

    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
    }

    suspend fun auth(network: NetworkType, token: String): AuthData {
        Logger.v("Loading", tag = TAG)

        val res: HttpResponse = httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
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
