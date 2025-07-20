package com.aglushkov.wordteacher.shared.apiproviders.wordteacher

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.WordServiceLogger
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.Response
import com.aglushkov.wordteacher.shared.general.getUserAgent
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
import com.aglushkov.wordteacher.shared.service.installErrorTracker
import com.aglushkov.wordteacher.shared.service.installLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.Locale

@Serializable
data class WordTeacherDictWordsResponse(
    @SerialName("words") val words: List<WordTeacherDictWord>?,
)

@Serializable
data class WordTeacherDictTextSearchResponse(
    @SerialName("words") val words: List<WordTeacherDictWord>?,
)

@Serializable
data class WordTeacherDictWord(
    @SerialName("term") val word: String,
    @SerialName("transcriptions") val transcriptions: List<String>?,
    @SerialName("audioFiles") val audioFiles: List<AudioFile> = emptyList(),
    @SerialName("defPairs") val defPairs: List<DefPair>,
) {
    @Serializable
    data class AudioFile(
        @SerialName("url") val url: String,
        @SerialName("accent") val accent: String?,
        @SerialName("transcription") val transcription: String?,
        @SerialName("text") val text: String?,
    )

    @Serializable
    data class DefPair(
        @SerialName("partOfSpeech") val partOfSpeech: WordTeacherWord.PartOfSpeech = PartOfSpeech.Undefined,
        @SerialName("defEntries") val defEntries: List<DefEntry>,
    )

    @Serializable
    data class DefEntry(
        @SerialName("definition") val definition: Definition,
        @SerialName("examples") val examples: List<String>?,
        @SerialName("synonyms") val synonyms: List<String>?,
        @SerialName("antonyms") val antonyms: List<String>?,
    ) {
        fun toWordTeacherDefinition(): WordTeacherDefinition {
            return WordTeacherDefinition(
                definitions = listOf(definition.value),
                examples = examples,
                synonyms = synonyms,
                antonyms = antonyms,
                imageUrl = null,
                labels = definition.labels,
            )
        }
    }

    @Serializable
    data class Definition(
        @SerialName("value") val value: String,
        @SerialName("labels") val labels: List<String>?,
    )

    fun toWordTeacherWord(): WordTeacherWord {
        return WordTeacherWord(
            word = word,
            transcriptions = transcriptions,
            definitions = LinkedHashMap<PartOfSpeech, List<WordTeacherDefinition>>().apply {
                defPairs.map { defPair ->
                    put(
                        defPair.partOfSpeech,
                        defPair.defEntries.map {
                            it.toWordTeacherDefinition()
                        }
                    )
                }
            },
            types = listOf(Config.Type.WordTeacher), // TODO: provide type from backend ("Wiktionary")
            audioFiles = audioFiles.map {
                WordTeacherWord.AudioFile(
                    url = it.url,
                    accent = it.accent,
                    transcription = it.transcription,
                    text = it.text,
                )
            },
        )
    }
}

class WordTeacherDictService (
    private val baseUrl: String,
    private val deviceIdRepository: DeviceIdRepository,
    private val appInfo: AppInfo,
    private val analyticsProvider: () -> Analytics,
    isDebug: Boolean,
) {
    companion object {}

    private val logger = WordServiceLogger(Config.Type.WordTeacher.name)
    private val httpClient = HttpClient {
        installHeaders()
        installLogger(isDebug)
        installErrorTracker(analyticsProvider)
        install(ContentEncoding) {
            gzip(0.9F)
        }
    }

    private val dictWordsJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            explicitNulls = false
            coerceInputValues = true
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(WordTeacherDictWordsResponse.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun loadWords(words: List<String>): Response<WordTeacherDictWordsResponse> {
        return withContext(Dispatchers.IO) {
            val terms = words.joinToString(",") { it.lowercase(Locale.getDefault()) }
            logger.logLoadingStarted(terms)
            val res: HttpResponse = httpClient.get("${baseUrl}/api/v3/dict/words", block = {
                parameter("terms", terms)
            })
            val responseString: String = res.body()
            logger.logLoadingCompleted(terms.encodeURLQueryComponent(), res, responseString)
            dictWordsJson.decodeFromString(responseString)
        }
    }

    private val dictTextSearchJson by lazy {
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "status"
            explicitNulls = false
            coerceInputValues = true
            serializersModule = SerializersModule {
                polymorphic(Response::class) {
                    subclass(Response.Ok.serializer(WordTeacherDictTextSearchResponse.serializer()))
                    subclass(Response.Err.serializer())
                }
            }
        }
    }

    suspend fun textSearch(text: String): Response<WordTeacherDictTextSearchResponse> {
        return withContext(Dispatchers.IO) {
            logger.logLoadingStarted(text)
            val res: HttpResponse = httpClient.get("${baseUrl}/api/v2/dict/words/textsearch/${text}")
            val responseString: String = res.body()
            logger.logLoadingCompleted(text, res, responseString)
            dictTextSearchJson.decodeFromString(responseString)
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

fun createWordTeacherWordService(
    service: WordTeacherDictService
): WordTeacherWordService {
    return object : WordTeacherWordService {
        override var type: Config.Type = Config.Type.WordTeacher

        override suspend fun define(words: List<String>): List<WordTeacherWord> {
            return service.loadWords(words).toOkResponse().words.orEmpty().map {
                it.toWordTeacherWord()
            }
        }
    }
}
