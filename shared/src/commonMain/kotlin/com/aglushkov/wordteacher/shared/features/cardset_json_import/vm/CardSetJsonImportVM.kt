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
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

interface CardSetJsonImportVM: Clearable {
    val jsonText: StateFlow<String>
    val jsonTextErrorFlow: Flow<StringDesc?>
    val eventFlow: Flow<Event>

    fun onJsonTextChanged(text: String)
    fun onCompletePressed()
    fun onCancelPressed(): Job
}

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
    }

    override fun onJsonTextChanged(text: String) {
        jsonText.value = text
        jsonTextErrorFlow.value = null
    }

    override fun onCompletePressed() {
        val cardSet: CardSet
        try {
            cardSet = json.decodeFromString<CardSet>(jsonText.value)
        } catch (e: Exception) {
            jsonTextErrorFlow.value = StringDesc.Raw(e.message.orEmpty())
            return
        }

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
