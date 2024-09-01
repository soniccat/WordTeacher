package com.aglushkov.wordteacher.shared.apiproviders.wordteacher

import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.getUserAgent
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.WordTeacherDefinition
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWord.PartOfSpeech
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.service.HeaderAppVersion
import com.aglushkov.wordteacher.shared.service.HeaderDeviceId
import com.aglushkov.wordteacher.shared.service.HeaderDeviceType
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import com.aglushkov.wordteacher.shared.service.installLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class WordTeacherDictResponse(
    @SerialName("words") val words: List<WordTeacherDictWord>?,
)

@Serializable
data class WordTeacherDictWord(
    @SerialName("term") val word: String,
    @SerialName("transcriptions") val transcriptions: List<String>?,
    @SerialName("defPairs") val defPairs: List<DefPair>,
) {
    @Serializable
    data class DefPair(
        @SerialName("partOfSpeech") val partOfSpeech: WordTeacherWord.PartOfSpeech,
        @SerialName("defEntries") val defEntries: List<DefEntry>,
    )

    @Serializable
    data class DefEntry(
        @SerialName("definition") val definition: Definition,
        @SerialName("examples") val examples: List<String>?,
        @SerialName("synonyms") val synonyms: List<String>?,
        @SerialName("antonyms") val antonyms: List<String>?,
    )

    @Serializable
    data class Definition(
        @SerialName("definition") val value: String,
        @SerialName("labels") val labels: List<String>?,
    )
}

class WordTeacherDictService (
    private val baseUrl: String,
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    isDebug: Boolean,
) {
    companion object {}

    private val logger = WordServiceLogger(Config.Type.WordTeacher.name)
    private val httpClient = HttpClient {
        installHeaders()
        installLogger(isDebug)
        install(ContentEncoding) {
            gzip(0.9F)
        }
    }

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
        return withContext(Dispatchers.Default) {
            logger.logLoadingStarted(word)
            val res: HttpResponse = httpClient.get("${baseUrl}/api/v2/dict/words/${word}")
            val responseString: String = res.body()
            logger.logLoadingCompleted(word, res, responseString)
            dictJson.decodeFromString(responseString)
        }
    }

    private fun HttpClientConfig<*>.installHeaders() {
        install(
            createClientPlugin("SpaceDictPlugin") {
                onRequest { request, _ ->
                    request.headers {
                        set(HeaderDeviceType, appInfo.osName)
                        set(HeaderAppVersion, appInfo.version)
                        set(HeaderDeviceId, deviceIdRepository.deviceId())
                        set(HttpHeaders.UserAgent, appInfo.getUserAgent())
                    }
                }
            }
        )
    }
}

fun WordTeacherDictService.Companion.createWordTeacherWordService(
    baseUrl: String,
    deviceIdRepository: DeviceIdRepository,
    appInfo: AppInfo,
    isDebug: Boolean,
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.WordTeacher
        private val service = WordTeacherDictService(baseUrl, deviceIdRepository, appInfo, isDebug)

        override suspend fun define(word: String): List<WordTeacherWord> {
            return service.loadWords(word.encodeURLQueryComponent()).toOkResponse().words.orEmpty().map {
                WordTeacherWord(
                    word = it.word,
                    transcriptions = it.transcriptions,
                    definitions = LinkedHashMap<PartOfSpeech, List<WordTeacherDefinition>>().apply {
                        it.defPairs.onEach { pair ->
                            put(pair.partOfSpeech, pair.defEntries.map { entry ->
                                WordTeacherDefinition(
                                    definitions = listOf(entry.definition.value),
                                    examples = entry.examples,
                                    synonyms = entry.synonyms,
                                    antonyms = entry.antonyms,
                                    imageUrl = null,
                                    labels = entry.definition.labels,
                                )
                            })
                        }
                    },
                    types = listOf(Config.Type.WordTeacher)
                )
            }
        }
    }
}
