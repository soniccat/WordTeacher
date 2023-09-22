package com.aglushkov.wordteacher.shared.apiproviders.wordteacher

import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class WordTeacherDictResponse(
    @SerialName("words") val words: List<WordTeacherWord>?,
)

class WordTeacherDictService (
    private val baseUrl: String
) {
    companion object {}

    private val logger = WordServiceLogger(Config.Type.WordTeacher.name)
    private val httpClient = HttpClient()

    private val dictJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            explicitNulls = false
            coerceInputValues = true
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(WordTeacherDictResponse.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun loadWords(word: String): Response<WordTeacherDictResponse> {
        logger.logLoadingStarted(word)

        val res: HttpResponse = httpClient.get("${baseUrl}/api/dict/words/${word}")
        return withContext(Dispatchers.Default) {
            val responseString = res.readBytes().decodeToString()
            logger.logLoadingCompleted(word, res, responseString)
            dictJson.decodeFromString(responseString)
        }
    }
}

fun WordTeacherDictService.Companion.createWordTeacherWordService(
    aBaseUrl: String,
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.WordTeacher
        private val service = WordTeacherDictService(aBaseUrl)

        override suspend fun define(word: String): List<WordTeacherWord> {
            return service.loadWords(word.encodeURLQueryComponent()).toOkResponse().words.orEmpty()
        }
    }
}
