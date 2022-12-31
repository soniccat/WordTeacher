package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
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
    private val baseUrl: String
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

    private val httpClient = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
    }

    suspend fun auth(network: NetworkType, token: String): AuthData {
        Logger.v("Loading", tag = TAG)

        val res: HttpResponse = httpClient.post(urlString = "${baseUrl}/api/auth/social/" + network.value) {
            this.body = json.encodeToString(Input(token))
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
