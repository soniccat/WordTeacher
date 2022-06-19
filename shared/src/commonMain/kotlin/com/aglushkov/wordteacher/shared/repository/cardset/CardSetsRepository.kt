package com.aglushkov.wordteacher.shared.repository.cardset

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.model.nlp.NLPSpan
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CardSetsRepository(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val nlpCore: NLPCore,
    private val nlpSentenceProcessor: NLPSentenceProcessor
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val cardSets = database.cardSets.selectAll().stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    init {

    }

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
    ): Card {
        val nlpCoreCopy = nlpCore.clone()
        return scope.async(Dispatchers.Default) {
            val definitionSpans = definitions.map { sentence ->
                findTermSpans(sentence, term, nlpCoreCopy)
            }

            database.cards.insertCard(
                setId = setId,
                term = term,
                date = timeSource.getTimeInMilliseconds(),
                definitions = definitions,
                definitionTermSpans = definitionSpans,
                partOfSpeech = partOfSpeech,
                transcription = transcription,
                synonyms = synonyms,
                examples = examples
            )
        }.await()
    }

    private fun findTermSpans(sentence: String, term: String, nlpCore: NLPCore): List<Pair<Int,Int>> {
        val nlpSentence = nlpSentenceProcessor.processString(sentence, nlpCore)
        val words = term.split(' ')
        var tokenI = 0
        var wordI = 0

        val foundTokenSpans: MutableList<NLPSpan> = mutableListOf()
        val termTokenSpans: MutableList<NLPSpan> = mutableListOf()

        while (tokenI < nlpSentence.tokenSpans.size && wordI < words.size) {
            val word = words[wordI]
            val token = nlpSentence.token(tokenI)
            val lemma = nlpSentence.lemma(tokenI)

            if (token == word || lemma == word) {
                termTokenSpans.add(nlpSentence.tokenSpans[tokenI])

                if (wordI == words.size - 1) {
                    foundTokenSpans.addAll(termTokenSpans)
                    termTokenSpans.clear()
                    wordI = 0
                } else {
                    ++wordI
                }
            }

            ++tokenI
        }

        return foundTokenSpans.map {
            Pair(it.start, it.end)
        }
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
