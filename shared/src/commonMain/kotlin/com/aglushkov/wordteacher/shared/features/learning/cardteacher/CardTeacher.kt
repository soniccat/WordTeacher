package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CardTeacher(
    private val cards: List<Card>,
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val scope: CoroutineScope
) {
    private var sessions = mutableListOf<LearnSession>()
    private var currentSessionStateFlow = MutableStateFlow<LearnSession?>(null)
    private var currentCardStateFlow = MutableStateFlow<MutableCard?>(null)

    val currentSession: LearnSession?
        get() = currentSessionStateFlow.value
    val currentSessionFlow: Flow<LearnSession>
        get() = currentSessionStateFlow.takeWhileNonNull()
    val currentCard: Card?
        get() = currentCardStateFlow.value
    val currentCardFlow: Flow<Card>
        get() = currentCardStateFlow.takeWhileNonNull()

    private var checkCount = 0
    private var hintShowCount = 0
    var isWrongAnswerCounted = false
        private set

    fun buildCourseSession() {
        val rightAnsweredCardSet = sessions.map { session ->
            session.rightAnsweredCards().map { it.id }
        }.flatten().toSet()

        val sessionCards = cards.filter { card ->
            !rightAnsweredCardSet.contains(card.id)
        }.shuffled().take(CARD_PER_SESSION)

        if (sessionCards.isEmpty()) {
            currentSessionStateFlow.value = null
        } else {
            val session = LearnSession(sessionCards.map { it.toMutableCard() })
            currentSessionStateFlow.value = session

            scope.launch {
                session.currentCardFlow.collect(currentCardStateFlow)
            }
        }
    }

    private fun prepareToNewCard() {
        isWrongAnswerCounted = false
        checkCount = 0
        hintShowCount = 0
    }

    suspend fun onCheckInput(answer: String): Boolean {
        ++checkCount

        val isRight = currentCard!!.term == answer
        if (isRight) {
            countRightAnswer()

        } else if (checkCount > 1) {
            countWrongAnswer()
        }

        return isRight
    }

    suspend fun onGiveUp() = countWrongAnswer()

    suspend fun onHintShown() {
        hintShowCount++

        if (hintShowCount > 1) {
            countWrongAnswer()
        }
    }

    private suspend fun countWrongAnswer() {
        val localCurrentCard = currentCard
        val currentCardSnapshot = localCurrentCard?.toImmutableCard()
        val mutableCard = localCurrentCard?.toMutableCard()

        if (currentCardSnapshot != null && mutableCard != null && !isWrongAnswerCounted) {
            databaseWorker.run {
                try {
                    mutableCard.progress.applyRightAnswer(timeSource)
                    database.cards.updateCard(mutableCard)
                } catch (e: Throwable) {
                    // TODO: handle error
                    Logger.e("CardTeacher.countWrongAnswer", e.toString())
                }
            }

            if (currentCardSnapshot == currentCard?.toImmutableCard()) {
                currentSessionStateFlow.value!!.updateProgress(mutableCard, false)
                isWrongAnswerCounted = true
            }

            switchToNextCard()
        }
    }

    private suspend fun countRightAnswer() {
        val localCurrentCard = currentCard
        val currentCardSnapshot = localCurrentCard?.toImmutableCard()
        val mutableCard = localCurrentCard?.toMutableCard()

        if (currentCardSnapshot != null && mutableCard != null) {
            databaseWorker.run {
                try {
                    mutableCard.progress.applyRightAnswer(timeSource)
                    database.cards.updateCard(mutableCard)
                } catch (e: Throwable) {
                    // TODO: handle error
                    Logger.e("CardTeacher.countRightAnswer", e.toString())
                }
            }

            if (currentCardSnapshot == currentCard?.toImmutableCard()) {
                currentCardStateFlow.value?.set(mutableCard)
                currentSessionStateFlow.value!!.updateProgress(mutableCard, true)
            }

            switchToNextCard()
        }
    }

    private fun switchToNextCard(): Card? {
        prepareToNewCard()
        val nextCard = currentSessionStateFlow.value?.switchToNextCard()
        if (nextCard == null) {
            sessions.add(currentSessionStateFlow.value!!)
            currentSessionStateFlow.value = null
        }
        return nextCard
    }
}

private const val CARD_PER_SESSION = 10