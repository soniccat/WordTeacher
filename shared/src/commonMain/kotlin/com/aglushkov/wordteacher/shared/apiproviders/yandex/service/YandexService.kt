package com.aglushkov.wordteacher.apiproviders.yandex.service

import com.aglushkov.wordteacher.apiproviders.yandex.model.YandexWords
import com.aglushkov.wordteacher.apiproviders.yandex.model.asWordTeacherWord
import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.ktor.CustomParameter
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.encodeURLQueryComponent
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class YandexService(
    private val baseUrl: String,
    private val apiKey: String,
    private val secureCodec: SecureCodec,
) {
    companion object {
        val Lookup = "yandex_lookup"
        val LookupLang = "yandex_lookup_lang"
        val LookupLangDefault = "en-en"
        val LookupUi = "yandex_lookup_ui"
        val LookupFlags = "yandex_lookup_flags"
    }

    private val logger = WordServiceLogger(Config.Type.Yandex.name)
    private val httpClient = HttpClient {
        install(CustomParameter) {
            parameterName = "key"
            parameterProvider = {
                secureCodec.decript(apiKey.decodeBase64Bytes())!!.decodeToString()
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun definitions(
        word: String,
        languages: String,
        uiLang: String,
        flags: Int
    ): YandexWords {
        logger.logLoadingStarted(word)

        val res: HttpResponse = httpClient.get("${baseUrl}api/v1/dicservice.json/lookup") {
            parameter("text", word)
            parameter("lang", languages)
            parameter("ui", uiLang)
            parameter("flags", flags)
        }
        return withContext(Dispatchers.Default) {
            val responseString: String = res.body()
            logger.logLoadingCompleted(word, res, responseString)
            json.decodeFromString(responseString)
        }
    }
}

fun YandexService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
    aKey: String,
    params: ServiceMethodParams,
    secureCodec: SecureCodec,
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.Yandex
        private val service = YandexService(aBaseUrl, aKey, secureCodec)

        override suspend fun define(word: String): List<WordTeacherWord> {
            val lookup = params.value[Lookup]
            val lang = lookup?.get(LookupLang) ?: LookupLangDefault
            val ui = lookup?.get(LookupUi) ?: "en"
            val flags = lookup?.get(LookupFlags)?.toIntOrNull() ?: 4

            return service.definitions(word.encodeURLQueryComponent(), lang, ui, flags).words.mapNotNull { it.asWordTeacherWord() }
        }
    }
}
