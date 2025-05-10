package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.data
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.onLoaded
import com.aglushkov.wordteacher.shared.general.resource.onLoading
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.CardSpan
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWord.AudioFile
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.UNDEFINED_FREQUENCY
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CardSetsRepository(
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val cardSets = databaseWorker.database.cardSets.selectAll().stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    suspend fun createCardSet(name: String, date: Long, infoSource: String? = null) = supervisorScope {
        // Async in the scope to avoid retaining the parent coroutine and to cancel immediately
        // when it cancels (when corresponding ViewModel is cleared for example)
        scope.async(Dispatchers.Default) {
            databaseWorker.run {
                it.cardSets.insert(name, date, infoSource)
            }
        }.await()
    }

    suspend fun insertCardSet(cardSet: CardSet) = supervisorScope {
        scope.async(Dispatchers.Default) {
            databaseWorker.run {
                it.cardSets.insert(cardSet)
            }
        }.await()
    }

    suspend fun removeCardSet(cardSetId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeCardSetInternal(cardSetId)
        }.await()
    }

    private fun removeCardSetInternal(cardSetId: Long) {
        databaseWorker.launch {
            it.cardSets.run {
                removeCardSet(cardSetId)
            }
        }
    }

    suspend fun addCard(
        setId: Long,
        term: String,
        definitions: List<String>,
        labels: List<String>,
        partOfSpeech: WordTeacherWord.PartOfSpeech,
        transcriptions: List<String>?,
        synonyms: List<String>,
        examples: List<String>,
        termFrequency: Double?,
        audioFiles: List<AudioFile>
    ): Card {
        return scope.async(Dispatchers.Default) {
            databaseWorker.run {
                it.cards.insertCard(
                    setId = setId,
                    term = term,
                    creationDate = timeSource.timeInstant(),
                    definitions = definitions,
                    labels = labels,
                    partOfSpeech = partOfSpeech,
                    transcriptions = transcriptions,
                    synonyms = synonyms,
                    examples = examples,
                    needToUpdateExampleSpans = true,
                    needToUpdateDefinitionSpans = true,
                    termFrequency = termFrequency,
                    audioFiles = audioFiles,
                )
            }
        }.await()
    }
}
