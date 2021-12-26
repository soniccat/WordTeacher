package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase

class CardTeacher(
    private val database: AppDatabase,
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

        currentSession = LearnSession(sessionCards)
    }

    private fun prepareToNewCard() {
        isWrongAnswerCounted = false
        checkCount = 0
        hintShowCount = 0
    }

    fun onCheckInput() = ++checkCount
    fun onRightInput() = countRightAnswer()
    fun onGiveUp() = countWrongAnswer()

    fun onWrongInput() {
        if (checkCount > 1) {
            countWrongAnswer()
        }
    }

    fun onHintShown() {
        hintShowCount++
        if (hintShowCount > 1) {
            countWrongAnswer()
        }
    }

    fun onSessionsFinished() {
        sessions.add(currentSession)
        buildCourseSession()
    }

    private fun countWrongAnswer() {
        val safeCurrentCard = currentCard
        if (safeCurrentCard != null && !isWrongAnswerCounted) {
            courseHolder.countWrongAnswer(safeCurrentCard)
            currentSession.updateProgress(safeCurrentCard, false)
            isWrongAnswerCounted = true
        }
    }

    private fun countRightAnswer() {
        val safeCurrentCard = currentCard
        if (safeCurrentCard != null) {

            database.cards.updateCard()

            safeCurrentCard.progress.applyRightAnswer(timeSource)

            courseHolder.countRighAnswer(currentCard)
            currentSession.updateProgress(safeCurrentCard, true)
        }
    }

    val currentCard: Card?
        get() = currentSession.currentCard

    fun nextCard(): Card? {
        prepareToNewCard()
        return currentSession.nextCard()
    }
}

private const val CARD_PER_SESSION = 10