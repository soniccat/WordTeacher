package com.aglushkov.wordteacher.shared.features.cardset_json_import.vm

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
import com.aglushkov.wordteacher.shared.general.exception
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardProgress
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.CardSpan
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.UNDEFINED_FREQUENCY
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

interface CardSetJsonImportVM: Clearable {
    val jsonText: StateFlow<String>
    val jsonTextErrorFlow: Flow<StringDesc?>
    val eventFlow: Flow<Event>

    fun onJsonTextChanged(text: String)
    fun onCompletePressed()
    fun onCancelPressed(): Job
}

@Serializable
data class ImportCardSet (
    val name: String,
    val cards: List<ImportCard> = emptyList(),
    var terms: List<String> = emptyList(), // for cardsets from search
)

@Serializable
data class ImportCard (
    val term: String,
    val definitions: List<String>?,
    val transcription: String?,
    val synonyms: List<String>?,
    val examples: List<String>?,
)

open class CardSetJsonImportVMImpl(
    val configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration,
    val cardSetsRepository: CardSetsRepository,
    val timeSource: TimeSource,
): ViewModel(), CardSetJsonImportVM {
    override val jsonText = MutableStateFlow("")
    override val jsonTextErrorFlow = MutableStateFlow<StringDesc?>(null)

    private val eventChannel = Channel<Event>(Channel.BUFFERED) // TODO: replace with a list of strings
    override val eventFlow = eventChannel.receiveAsFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    override fun onJsonTextChanged(text: String) {
        jsonText.value = text
        jsonTextErrorFlow.value = null
    }

    override fun onCompletePressed() {
        val importCardSet: ImportCardSet
        try {
            importCardSet = json.decodeFromString<ImportCardSet>(jsonText.value)
        } catch (e: Exception) {
            jsonTextErrorFlow.value = StringDesc.Raw(e.message.orEmpty())
            return
        }

        val nowTime = timeSource.timeInstant()
        val cardSet = CardSet(
            id = 0,
            remoteId = "",
            name = importCardSet.name,
            creationDate = nowTime,
            modificationDate = nowTime,
            cards = importCardSet.cards.map {
                Card(
                    id = 0,
                    remoteId = "",
                    creationDate = nowTime,
                    modificationDate = nowTime,
                    term = it.term,
                    definitions = it.definitions.orEmpty(),
                    definitionTermSpans = listOf(),
                    partOfSpeech = WordTeacherWord.PartOfSpeech.Undefined,
                    transcription = it.transcription,
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
                    creationId = UUID.randomUUID().toString(),
                    termFrequency = UNDEFINED_FREQUENCY
                )
            },
            terms = listOf(),
            creationId = UUID.randomUUID().toString()
        )

        createCardSet(cardSet)
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
            Logger.exception(e, TAG)
            val errorText = e.message?.let {
                StringDesc.Raw(it)
            } ?: StringDesc.Resource(MR.strings.error_default)

            eventChannel.trySend(ErrorEvent(errorText))
        }
    }
}

private const val TAG = "CardSetJsonImportVM"
