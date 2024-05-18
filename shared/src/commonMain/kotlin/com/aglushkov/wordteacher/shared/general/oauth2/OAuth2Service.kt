package com.aglushkov.wordteacher.shared.general.oauth2

import com.aglushkov.wordteacher.shared.general.crypto.PkceGenerator
import com.benasher44.uuid.uuid4
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OAuth2Service(
    private val authUrl: Url,
    private val tokenUrl: Url,

    private val clientId: String,
    private val clientSecret: String,
    private val redirectUrl: Url,
    private val scope: String = "profile",
    private val pkceGenerator: PkceGenerator = PkceGenerator(),

    private val responseType: String = "code",
    private val grantType: String = "authorization_code"
) {
    private val httpClient = HttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    data class AuthContext(
        val url: Url,
        val codeVerifier: String,
        val state: String,
    )

    sealed interface AuthResult {
        data class Success(val code: String): AuthResult
        data class Error(val error: String): AuthResult
        object WrongUrl: AuthResult
        object WrongState: AuthResult
        object UnexpectedRedirectParameters: AuthResult
        data class Unknown(val throwable: Throwable?): AuthResult
    }

    @Serializable
    data class Token(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Int,
        @SerialName("id_token") val idToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("scope") val scope: String? = null,
        @SerialName("token_type") val tokenType: String,
    )

    fun buildAuthContext(
        extraParameters: StringValues? = null,
    ): AuthContext {
        val codeVerifier = pkceGenerator.codeVerifier()
        val state = uuid4().toString()
        val resultUrl = URLBuilder(authUrl).apply {
            extraParameters?.let {
                parameters.appendMissing(it)
            }
            parameters.append("code_challenge", pkceGenerator.codeChallenge(codeVerifier))
            parameters.append("code_challenge_method", PkceGenerator.CODE_CHALLENGE_METHOD)
            parameters.append("client_id", clientId)
            parameters.append("scope", scope)
            parameters.append("redirect_uri", redirectUrl.toString())
            parameters.append("response_type", responseType)
            parameters.append("state", state)
        }.build()

        return AuthContext(resultUrl, codeVerifier, state)
    }

    fun parseAuthResponseUrl(urlStr: String, state: String): AuthResult {
        val url = try {
            Url(urlStr)
        } catch (e: Exception) {
            return AuthResult.Unknown(e)
        }

        if (url.host != redirectUrl.host) {
            return AuthResult.WrongUrl
        }

        url.parameters["error"]?.let {
            return AuthResult.Error(it)
        }
        if (url.parameters["state"] != state) {
            return AuthResult.WrongState
        }
        url.parameters["code"]?.let {
            return AuthResult.Success(it)
        }

        return AuthResult.UnexpectedRedirectParameters
    }

    suspend fun accessToken(code: String, context: AuthContext): Token {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse =
                httpClient.post(tokenUrl) {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("client_id", clientId)
                                append("client_secret", clientSecret)
                                append("code", code)
                                append("code_verifier", context.codeVerifier)
                                append("grant_type", grantType)
                                append("redirect_uri", redirectUrl.toString())
                            }
                        )
                    )
                }
            val stringResponse: String = res.body()
            json.decodeFromString(stringResponse)
        }
    }
}
