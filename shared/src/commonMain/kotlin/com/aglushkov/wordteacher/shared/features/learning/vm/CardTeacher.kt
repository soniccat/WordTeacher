package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
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
    private var sessions = mutableListOf<LearningSession>()
    private var currentSession: LearningSession? = null
    private var currentCardStateFlow = MutableStateFlow<MutableCard?>(null)

    val currentCard: Card?
        get() = currentCardStateFlow.value
    val currentCardFlow: Flow<Card>
        get() = currentCardStateFlow.takeWhileNonNull()

    private var checkCount = 0
    private var hintShowCount = 0
    var isWrongAnswerCounted = false
        private set

    suspend fun runSession(block: suspend (cards: Flow<Card>) -> Unit): List<SessionCardResult>? {
        val session = buildLearnSession()
        if (session != null) {
            block(currentCardFlow)
        }

        return session?.results
    }

    private fun buildLearnSession(): LearningSession? {
        val rightAnsweredCardSet = sessions.map { session ->
            session.rightAnsweredCards().map { it.id }
        }.flatten().toSet()

        val sessionCards = cards.filter { card ->
            !rightAnsweredCardSet.contains(card.id)
        }.shuffled().take(CARD_PER_SESSION)

        if (sessionCards.isEmpty()) {
            currentSession = null
        } else {
            val session = LearningSession(sessionCards.map { it.toMutableCard() })
            currentSession = session

            scope.launch {
                session.currentCardFlow.collect(currentCardStateFlow)
                currentCardStateFlow.value = null // to propagate cancellation
            }
        }

        return currentSession
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

            val safeSession = currentSession
            if (safeSession != null && currentCardSnapshot == currentCard?.toImmutableCard()) {
                safeSession.updateProgress(mutableCard, false)
                isWrongAnswerCounted = true
            }
        }
    }

    private suspend fun countRightAnswer() {
        val localCurrentCard = currentCard
        val currentCardSnapshot = localCurrentCard?.toImmutableCard()
        val mutableCard = localCurrentCard?.toMutableCard()

        if (currentCardSnapshot != null && mutableCard != null) {
            if (!isWrongAnswerCounted) {
                databaseWorker.run {
                    try {
                        mutableCard.progress.applyRightAnswer(timeSource)
                        database.cards.updateCard(mutableCard)
                    } catch (e: Throwable) {
                        // TODO: handle error
                        Logger.e("CardTeacher.countRightAnswer", e.toString())
                    }
                }

                val safeSession = currentSession
                if (safeSession != null && currentCardSnapshot == currentCard?.toImmutableCard()) {
                    //currentCardStateFlow.value?.set(mutableCard)
                    safeSession.updateProgress(mutableCard, true)
                }
            }

            switchToNextCard()
        }
    }

    private fun switchToNextCard(): Card? {
        prepareToNewCard()
        val nextCard = currentSession?.switchToNextCard()
        val safeSession = currentSession

        if (safeSession != null && nextCard == null) {
            sessions.add(safeSession)
            currentSession = null
        }

        return nextCard
    }

    fun save() = State(
        sessionStates = sessions.map { it.save() },
        currentSessionState = currentSession?.save(),
        checkCount = checkCount,
        hintShowCount = hintShowCount,
        isWrongAnswerCounted = isWrongAnswerCounted
    )

    fun restore(state: State) {
        sessions = state.sessionStates.map(::createLearningSession).toMutableList()
        currentSession = state.currentSessionState?.let(::createLearningSession)
        checkCount = state.checkCount
        hintShowCount = state.hintShowCount
        isWrongAnswerCounted = state.isWrongAnswerCounted
    }

    private fun createLearningSession(sessionState: LearningSession.State) =
        LearningSession(
            cards = sessionState.cardIds.mapNotNull { cardId ->
                cards.firstOrNull { it.id == cardId }?.toMutableCard()
            }
        ).also {
            it.restore(sessionState)
        }

    @Parcelize
    data class State(
        val sessionStates: List<LearningSession.State>,
        val currentSessionState: LearningSession.State?,
        val checkCount: Int,
        val hintShowCount: Int,
        val isWrongAnswerCounted: Boolean
    ) : Parcelable
}

private const val CARD_PER_SESSION = 2