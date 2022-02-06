package com.aglushkov.wordteacher.apiproviders.google.service

import com.aglushkov.wordteacher.apiproviders.google.model.GoogleWord
import com.aglushkov.wordteacher.apiproviders.google.model.asWordTeacherWord
import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GoogleService(
    private val baseUrl: String
) {
    companion object {
        val Entries = "google_entries"
        val EntriesLang = "google_entries_lang"
    }

    private val logger = WordServiceLogger(Config.Type.Google.name)
    private val httpClient = HttpClient()

    suspend fun loadDefinitions(word: String, lang: String): List<GoogleWord> {
        logger.logLoadingStarted(word)

        val res: HttpResponse = httpClient.get("${baseUrl}api/v1/entries/${lang}/${word}")
        return withContext(Dispatchers.Default) {
            val responseString = res.readBytes().decodeToString()
            logger.logLoadingCompleted(word, res, responseString)

            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(responseString)
        }
    }
}

fun GoogleService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
    params: ServiceMethodParams
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.Google
        private val service = GoogleService(aBaseUrl)

        override suspend fun define(word: String): List<WordTeacherWord> {
            val lang: String = params.value[Entries]?.get(EntriesLang) ?: "en"
            return service.loadDefinitions(word, lang).mapNotNull { it.asWordTeacherWord() }
        }
    }
}