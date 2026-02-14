package com.aglushkov.wordteacher.shared.features.cardset_json_import.vm

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.events.CompletionData
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.CompletionResult
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.events.Event
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.ViewModel
import com.aglushkov.wordteacher.shared.general.article_parser.ParsedArticle
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.exception
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardProgress
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.CardSetInfo
import com.aglushkov.wordteacher.shared.model.CardSpan
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import com.aglushkov.wordteacher.shared.model.nlp.TokenSpan
import com.aglushkov.wordteacher.shared.model.nlp.toPartOfSpeech
import com.aglushkov.wordteacher.shared.repository.cardset.CardEnricher
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.UNDEFINED_FREQUENCY
import com.aglushkov.wordteacher.shared.res.MR
import com.benasher44.uuid.Uuid
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

interface CardSetJsonImportVM: Clearable {
    val jsonText: StateFlow<String>
    val jsonTextErrorFlow: Flow<StringDesc?>
    val eventFlow: Flow<Event>

    fun onJsonTextChanged(text: String)
    fun onEnrichClicked()
    fun onCheckClicked()
    fun onCompletePressed()
    fun onCancelPressed(): Job
}

@Serializable
data class ImportCardSet (
    val name: String,
    val cards: List<ImportCard> = emptyList(),
    var terms: List<String> = emptyList(), // for cardsets from search
    var info: CardSetInfo,
    var isAvailableInSearch: Boolean = false,
) {
    fun toCardSet(nowTime: Instant) = CardSet(
        id = 0,
        remoteId = "",
        name = name,
        creationDate = nowTime,
        modificationDate = nowTime,
        cards = cards.map {
            Card(
                id = 0,
                remoteId = "",
                creationDate = nowTime,
                modificationDate = nowTime,
                term = it.term,
                definitions = it.definitions.orEmpty(),
                labels = it.labels.orEmpty(),
                definitionTermSpans = listOf(),
                partOfSpeech = it.partOfSpeech,
                transcriptions = it.transcriptions.orEmpty(),
                synonyms = it.synonyms.orEmpty(),
                examples = it.examples.orEmpty(),
                exampleTermSpans = listOf(),
                progress = CardProgress(
                    currentLevel = 0,
                    lastMistakeCount = 0,
                    lastLessonDate = null
                ),
                needToUpdateDefinitionSpans = true,
                needToUpdateExampleSpans = true,
                creationId = Uuid.randomUUID().toString(),
                termFrequency = UNDEFINED_FREQUENCY,
                audioFiles = it.audioFiles.orEmpty(),
            )
        },
        terms = listOf(),
        creationId = Uuid.randomUUID().toString(),
        info = info,
        isAvailableInSearch = isAvailableInSearch,
    )
}

@Serializable
data class ImportCard (
    override var term: String,
    override var partOfSpeech: WordTeacherWord.PartOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
    var definitions: List<String>?,
    var labels: List<String>?,
    override var transcriptions: List<String>?,
    var synonyms: List<String>?,
    override var examples: List<String>?,
    override var audioFiles: List<WordTeacherWord.AudioFile>?,
) : CardEnricher.Target

open class CardSetJsonImportVMImpl(
    private val configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
    private val cardEnricher: CardEnricher,
): ViewModel(), CardSetJsonImportVM {
    override val jsonText = MutableStateFlow("")
    override val jsonTextErrorFlow = MutableStateFlow<StringDesc?>(null)

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        prettyPrint = true
    }

    private val httpClient = HttpClient {
    }

    private var loadTextJob: Job? = null
    override fun onJsonTextChanged(text: String) {
        jsonText.value = text
        jsonTextErrorFlow.value = null

        if (text.startsWith("http")) {
            loadTextJob?.cancel()
            loadTextJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val loadedText = loadText(text)
                    jsonText.value = loadedText
                } catch (e: Exception) {
                    jsonTextErrorFlow.value = StringDesc.Raw(e.message.orEmpty())
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun loadText(url: String): String {
        val res: HttpResponse = httpClient.get(url)
        val responseString: String = res.body()
        return responseString
    }

    override fun onCheckClicked() {
        val cardSet = createCardSetWithErrorHandling() ?: return
        jsonText.value = json.encodeToString(cardSet)
    }

    override fun onEnrichClicked() {
        val importCardSet: ImportCardSet = createCardSetWithErrorHandling() ?: return
        viewModelScope.launch {
            try {
                val newDataList = cardEnricher.enrich(importCardSet.cards)
                val enrichedCardSet = importCardSet.copy(
                    cards = importCardSet.cards.mapIndexed { index, importCard ->
                        val newData = newDataList[index]
                        importCard.copy(
                            transcriptions = newData.transcriptions ?: importCard.transcriptions,
                            audioFiles = newData.audioFiles ?: importCard.audioFiles,
                            partOfSpeech = newData.partOfSpeech,
                        )
                    }
                )

                jsonText.value = json.encodeToString(enrichedCardSet)

            } catch (e: Exception) {
                Logger.e(e.message.orEmpty(), TAG)
                jsonTextErrorFlow.value = StringDesc.Raw(e.message.orEmpty())
                e.printStackTrace()
            }
        }
    }

    override fun onCompletePressed() {
        val importCardSet: ImportCardSet = createCardSetWithErrorHandling() ?: return
        val nowTime = timeSource.timeInstant()
        createCardSet(importCardSet.toCardSet(nowTime))
    }

    private fun createCardSetWithErrorHandling(): ImportCardSet? {
        try {
            val cardSet = json.decodeFromString<ImportCardSet>(jsonText.value)
            return cardSet.copy(
                cards = cardSet.cards.map {
                    it.copy(
                        examples = it.examples?.map {
                            it.replace("\n", "")
                                .replace("\t", "")
                        }
                    )
                }
            )
        } catch (e: Exception) {
            Logger.e(e.message.orEmpty(), TAG)
            jsonTextErrorFlow.value = StringDesc.Raw(e.message.orEmpty())
            return null
        }
    }

    override fun onCancelPressed() = viewModelScope.launch {
        eventChannel.trySend(CompletionEvent(CompletionResult.CANCELLED))
    }

    private fun createCardSet(cardSet: CardSet) = viewModelScope.launch {
        runSafely {
            // TODO: show loading, adding might take for a while
            val article = cardSetsRepository.insertCardSet(cardSet)
            eventChannel.trySend(CompletionEvent(CompletionResult.COMPLETED, CompletionData.Article(article.id)))
        }
    }

    private suspend fun runSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.exception("CardSetJsonImportVM.runSafely", e, TAG)
            val errorText = e.message?.let {
                StringDesc.Raw(it)
            } ?: StringDesc.Resource(MR.strings.error_default)

            eventChannel.trySend(ErrorEvent(errorText))
        }
    }
}

private const val TAG = "CardSetJsonImportVM"
