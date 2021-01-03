package com.aglushkov.wordteacher.shared.apiproviders.owlbot.service

import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.ktor.CustomHeader
import com.aglushkov.wordteacher.shared.apiproviders.owlbot.model.OwlBotWord
import com.aglushkov.wordteacher.shared.apiproviders.owlbot.model.asWordTeacherWord
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OwlBotService(
    private val baseUrl: String,
    private val key: String
) {
    companion object {}

    private val logger = WordServiceLogger(Config.Type.OwlBot.name)
    private val httpClient = HttpClient {
        val aKey = key // to fix mutation attempt of frozen OwlBot@<address> as otherwise "this" is captured
        install(CustomHeader) {
            headerName = HttpHeaders.Authorization
            headerValue = "Token $aKey"
        }
    }

    suspend fun loadDefinition(word: String): OwlBotWord {
        logger.logLoadingStarted(word)

        val res: HttpResponse = httpClient.get("${baseUrl}api/v4/dictionary/${word}")
        return withContext(Dispatchers.Default) {
            val responseString = res.readBytes().decodeToString()
            logger.logLoadingCompleted(word, res, responseString)

            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(responseString)
        }
    }
}

fun OwlBotService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
    aKey: String
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.OwlBot
        override var key = aKey
        override var baseUrl = aBaseUrl
        override var methodParams = ServiceMethodParams(emptyMap())

        private val service = OwlBotService(aBaseUrl, aKey)

        override suspend fun define(word: String): List<WordTeacherWord> {
            return service.loadDefinition(word).asWordTeacherWord()?.let {
                listOf(it)
            } ?: emptyList()
        }
    }
}
