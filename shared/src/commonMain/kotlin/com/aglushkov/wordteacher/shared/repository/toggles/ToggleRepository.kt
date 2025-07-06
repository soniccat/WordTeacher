package com.aglushkov.wordteacher.shared.repository.toggles

import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.service.SpaceAuthData
import com.aglushkov.wordteacher.shared.service.SpaceAuthService.AuthInput
import com.aglushkov.wordteacher.shared.service.SpaceAuthToken
import com.aglushkov.wordteacher.shared.service.SpaceAuthUser
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Toggles(
    @SerialName("topLevelDomain") val topLevelDomain: String = "com",
)

class ToggleRepository(
    private val url: String,
    private val httpClient: HttpClient,
    private val settings: FlowSettings
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val toggles: Toggles

    init {
        toggles = try {
            val toggleString = settings.toBlockingSettings().getStringOrNull(TogglesLastLoadedToggles)
            toggleString?.let {
                json.decodeFromString<Toggles>(it)
            }
        } catch (e: Throwable) {
            null
        } ?: Toggles()

        mainScope.launch(Dispatchers.Default) {
            try {
                loadRemoteToggles()
            } catch (e: Exception) {
                // ignore, sends network logs in httpClient
            }
        }
    }

    val togglesAsString: String by lazy {
        json.encodeToString(toggles)
    }

    suspend fun loadRemoteToggles(): Toggles {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse =
                httpClient.get(urlString = url) {
                    contentType(ContentType.Application.Json)
                    settings.getStringOrNull(TogglesEtagKey)?.let { etag ->
                        headers {
                            append("If-None-Match", etag)
                        }
                    }
                }
            val stringResponse: String = res.body()
            val resultToggles = json.decodeFromString<Toggles>(stringResponse)
            settings.putString(TogglesLastLoadedToggles, stringResponse)
            res.headers["etag"]?.let {
                settings.putString(TogglesEtagKey, it)
            }
            resultToggles
        }
    }
}

private const val TogglesEtagKey = "TogglesEtag"
private const val TogglesLastLoadedToggles = "TogglesLastLoaded"
