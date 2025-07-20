package com.aglushkov.wordteacher.shared.repository.toggles

import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.settings.serializable
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
    private val url2: String,
    private val httpClient: HttpClient,
    private val settings: SettingStore
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val toggles: Toggles = settings.serializable(TogglesLastLoadedToggles) ?: Toggles()

    init {
        mainScope.launch(Dispatchers.Default) {
            try {
                loadRemoteToggles(url)
            } catch (e: Exception) {
                try {
                    loadRemoteToggles(url2)
                } catch (e: Exception) {
                    // ignore, sends network logs in httpClient
                }
            }
        }
    }

    val togglesAsString: String by lazy {
        json.encodeToString(toggles)
    }

    private suspend fun loadRemoteToggles(urlString: String): Toggles {
        return withContext(Dispatchers.Default) {
            val res: HttpResponse =
                httpClient.get(urlString = urlString) {
                    contentType(ContentType.Application.Json)
                    settings.string(TogglesEtagKey)?.let { etag ->
                        headers {
                            append("If-None-Match", etag)
                        }
                    }
                }
            val stringResponse: String = res.body()
            json.decodeFromString<Toggles>(stringResponse).also {
                settings[TogglesLastLoadedToggles] = stringResponse
                res.headers["etag"]?.let {
                    settings[TogglesEtagKey] = it
                }
            }
        }
    }
}

private const val TogglesEtagKey = "TogglesEtag"
private const val TogglesLastLoadedToggles = "TogglesLastLoaded"
