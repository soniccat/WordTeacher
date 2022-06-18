package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.extensions.asFlow
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.tryInResource
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.CardSet
import com.aglushkov.wordteacher.shared.model.ShortCardSet
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class CardSetsRepository(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val cardSets = database.cardSets.selectAll().stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    suspend fun createCardSet(name: String, date: Long) = supervisorScope {
        // Async in the scope to avoid retaining the parent coroutine and to cancel immediately
        // when it cancels (when corresponding ViewModel is cleared for example)
        scope.async(Dispatchers.Default) {
            database.cardSets.insert(name, date)
        }.await()
    }

    suspend fun removeCardSet(cardSetId: Long) = supervisorScope {
        scope.async(Dispatchers.Default) {
            removeCardSetInternal(cardSetId)
        }.await()
    }

    private fun removeCardSetInternal(cardSetId: Long) {
        database.cardSets.run {
            removeCardSet(cardSetId)
        }
    }

    suspend fun addCard(
        setId: Long,
        term: String,
        definitions: List<String>,
        partOfSpeech: WordTeacherWord.PartOfSpeech,
        transcription: String?,
        synonyms: List<String>,
        examples: List<String>
    ) {
        scope.async(Dispatchers.Default) {
            database.cards.insertCard(
                setId = setId,
                term = term,
                date = timeSource.getTimeInMilliseconds(),
                definitions = definitions,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples
            )
        }.await()
    }

    suspend fun allCardIds(): List<Long> {
        return databaseWorker.run {
            database.cards.selectAllCardIds().executeAsList()
        }
    }

    suspend fun allReadyToLearnCardIds(): List<Long> {
        return databaseWorker.run {
            database.cards.selectAllCards().executeAsList().filter {
                it.progress.isReadyToLearn(timeSource)
            }.map { it.id }
        }
    }
}
