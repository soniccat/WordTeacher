package com.aglushkov.wordteacher.apiproviders.google.service

import com.aglushkov.wordteacher.apiproviders.google.model.GoogleWord
import com.aglushkov.wordteacher.apiproviders.google.model.asWordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.Config
import com.aglushkov.wordteacher.shared.repository.ServiceMethodParams
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

    private val httpClient = HttpClient { }

    suspend fun loadDefinitions(word: String, lang: String): List<GoogleWord> {
        val res: HttpResponse = httpClient.get("${baseUrl}api/v1/entries/${lang}/${word}")
        return withContext(Dispatchers.Default) {
            val string = res.readBytes().decodeToString()
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(string)
        }
    }
}

fun GoogleService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
    params: ServiceMethodParams
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.Google
        override var key = ""
        override var baseUrl = aBaseUrl
        override var methodParams = params

        private val service = GoogleService(aBaseUrl)

        override suspend fun define(word: String): List<WordTeacherWord> {
            val lang: String = methodParams.value[Entries]?.get(EntriesLang) ?: "en"
            return service.loadDefinitions(word, lang).mapNotNull { it.asWordTeacherWord() }
        }
    }
}