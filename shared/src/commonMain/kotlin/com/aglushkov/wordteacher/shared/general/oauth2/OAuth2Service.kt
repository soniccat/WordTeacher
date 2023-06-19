package com.aglushkov.wordteacher.shared.general.oauth2

import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.crypto.PkceGenerator
import com.aglushkov.wordteacher.shared.general.setStatusCode
import com.aglushkov.wordteacher.shared.service.CardSetSearchResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class OAuth2Token(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("device_secret") val deviceSecret: String? = null,
    @SerialName("token_type") val tokenType: String,
)

class OAuth2Service private constructor(
    private val authUrl: Url,
    private val tokenUrl: Url,

    private val clientId: String,
    private val redirectUrl: Url,
    private val scope: String = "profile",
    private val pkceGenerator: PkceGenerator = PkceGenerator(),
    private val responseType: String = "code",
) {
    data class Context(
        val url: Url,
        val codeVerifier: String,
    )

    suspend fun accessToken(code: String): Response<CardSetSearchResponse> {
        val res: HttpResponse =
            httpClient.get(urlString = "${baseUrl}/api/cardsets/search") {
                url {
                    parameter("query", query)
                }
            }
        return withContext(Dispatchers.Default) {
            val stringResponse = res.readBytes().decodeToString()
            json.decodeFromString<Response<CardSetSearchResponse>>(stringResponse).setStatusCode(res.status.value)
        }
    }

    fun buildWebUrl(
        extraParameters: StringValues? = null,
    ): Context {
        val codeVerifier = pkceGenerator.codeVerifier()
        val resultUrl = URLBuilder(authUrl).apply {
            extraParameters?.let {
                parameters.appendMissing(it)
            }

            parameters.append("code_challenge", codeVerifier)
            parameters.append("code_challenge_method", PkceGenerator.CODE_CHALLENGE_METHOD)
            parameters.append("client_id", clientId)
            parameters.append("scope", scope)
            parameters.append("redirect_uri", redirectUrl)
            parameters.append("response_type", "code")
            parameters.append("state", state)
            parameters.append("nonce", nonce)

        }.build()


        for (entry in extraRequestParameters.entries) {
            urlBuilder.addQueryParameter(entry.key, entry.value)
        }

//        val maxAge = extraRequestParameters["max_age"]?.toIntOrNull()

        return Context(url = resultUrl, codeVerifier = codeVerifier/*, redirectUrl, codeVerifier, state, nonce, maxAge*/)
    }

    /**
     * Resumes the OIDC Authorization Code redirect flow.
     * This method takes the returned redirect [Uri], and communicates with the Authorization Server to exchange that for a token.
     *
     * @param uri the redirect [Uri] which includes the authorization code to complete the flow.
     * @param flowContext the [AuthorizationCodeFlow.Context] used internally to maintain state.
     */
    suspend fun resume(uri: Uri, flowContext: Context): OidcClientResult<Token> {
        if (!uri.toString().startsWith(flowContext.redirectUrl)) {
            return OidcClientResult.Error(RedirectSchemeMismatchException())
        }

        val errorQueryParameter = uri.getQueryParameter("error")
        if (errorQueryParameter != null) {
            val errorDescription = uri.getQueryParameter("error_description") ?: "An error occurred."
            return OidcClientResult.Error(ResumeException(errorDescription, errorQueryParameter))
        }

        val stateQueryParameter = uri.getQueryParameter("state")
        if (flowContext.state != stateQueryParameter) {
            val error = "Failed due to state mismatch."
            return OidcClientResult.Error(ResumeException(error, "state_mismatch"))
        }

        val code = uri.getQueryParameter("code") ?: return OidcClientResult.Error(MissingResultCodeException())

        val endpoints = oidcClient.endpointsOrNull() ?: return oidcClient.endpointNotAvailableError()

        val formBodyBuilder = FormBody.Builder()
            .add("redirect_uri", flowContext.redirectUrl)
            .add("code_verifier", flowContext.codeVerifier)
            .add("client_id", oidcClient.configuration.clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)

        val request = Request.Builder()
            .post(formBodyBuilder.build())
            .url(endpoints.tokenEndpoint)
            .build()

        return oidcClient.tokenRequest(request, flowContext.nonce, flowContext.maxAge)
    }
}
