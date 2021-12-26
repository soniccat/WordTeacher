package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker

class CardTeacher(
    private val database: AppDatabase,
    private val databaseWorker: DatabaseWorker,
    private val timeSource: TimeSource,
    private val cards: List<Card>
) {
    private var sessions = mutableListOf<LearnSession>()
    private lateinit var currentSession: LearnSession
        private set
    private var checkCount = 0
    private var hintShowCount = 0
    var isWrongAnswerCounted = false
        private set

    init {
        buildCourseSession()
    }

    private fun buildCourseSession() {
        val rightAnsweredCardSet = sessions.map { session ->
            session.rightAnsweredCards().map { it.id }
        }.flatten().toSet()

        val sessionCards = cards.filter { card ->
            !rightAnsweredCardSet.contains(card.id)
        }.shuffled().take(CARD_PER_SESSION)

        currentSession = LearnSession(sessionCards.map { it.toMutableCard() })
    }

    private fun prepareToNewCard() {
        isWrongAnswerCounted = false
        checkCount = 0
        hintShowCount = 0
    }

    fun onCheckInput() = ++checkCount
    suspend fun onRightInput() = countRightAnswer()
    suspend fun onGiveUp() = countWrongAnswer()

    suspend fun onWrongInput() {
        if (checkCount > 1) {
            countWrongAnswer()
        }
    }

    suspend fun onHintShown() {
        hintShowCount++
        if (hintShowCount > 1) {
            countWrongAnswer()
        }
    }

    fun onSessionsFinished() {
        sessions.add(currentSession)
        buildCourseSession()
    }

    private suspend fun countWrongAnswer() {
        val localCurrentCard = currentCard
        val currentCardSnapshot = localCurrentCard?.toImmutableCard()
        val mutableCard = localCurrentCard?.toMutableCard()

        if (currentCardSnapshot != null && mutableCard != null && !isWrongAnswerCounted) {
            databaseWorker.run {
                try {
                    database.cards.updateCard(mutableCard)
                    mutableCard.progress.applyRightAnswer(timeSource)
                } catch (e: Throwable) {
                    // TODO: handle error
                    Logger.e("CardTeacher.countWrongAnswer", e.toString())
                }
            }

            if (currentCardSnapshot == currentCard?.toImmutableCard()) {
                currentSession.updateProgress(mutableCard, false)
                isWrongAnswerCounted = true
            }
        }
    }

    private suspend fun countRightAnswer() {
        val localCurrentCard = currentCard
        val currentCardSnapshot = localCurrentCard?.toImmutableCard()
        val mutableCard = localCurrentCard?.toMutableCard()

        if (currentCardSnapshot != null && mutableCard != null) {
            databaseWorker.run {
                try {
                    database.cards.updateCard(mutableCard)
                    mutableCard.progress.applyRightAnswer(timeSource)
                } catch (e: Throwable) {
                    // TODO: handle error
                    Logger.e("CardTeacher.countRightAnswer", e.toString())
                }
            }

            if (currentCardSnapshot == currentCard?.toImmutableCard()) {
                currentCard?.set(mutableCard)
                currentSession.updateProgress(mutableCard, true)
            }
        }
    }

    val currentCard: MutableCard?
        get() = currentSession.currentCard

    fun nextCard(): Card? {
        prepareToNewCard()
        return currentSession.nextCard()
    }
}

private const val CARD_PER_SESSION = 10