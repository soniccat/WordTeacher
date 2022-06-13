package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
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
    private var testSessions = mutableListOf<TestSession>()
    private var currentTestSession: TestSession? = null
    private var currentTestCardStateFlow = MutableStateFlow<TestSession.TestCard?>(null)
    private val currentTestCard: TestSession.TestCard?
        get() = currentTestCardStateFlow.value

    private var sessions = mutableListOf<LearningSession>()
    private var currentSession: LearningSession? = null
    private var currentCardStateFlow = MutableStateFlow<Card?>(null)
    private val currentCard: Card?
        get() = currentCardStateFlow.value

    private var hintStringStateFlow = MutableStateFlow<List<Char>>(emptyList())
    private var hintShowCountStateFlow = MutableStateFlow(0)

    val hintString: Flow<List<Char>>
        get() = hintStringStateFlow
    val hintShowCount: Flow<Int>
        get() = hintShowCountStateFlow

    private var checkCount = 0
    var isWrongAnswerCounted = false
        private set

    suspend fun runSession(
        block: suspend (count:Int, testCards: Flow<TestSession.TestCard>?, cards: Flow<Card>) -> Unit
    ): List<SessionCardResult>? {
        val session = buildLearnSession() ?: return null

        if (session.cards.size >= TEST_SESSION_OPTION_COUNT) {
            val testSession = TestSession(session.cards, cards.map { it.term })
            currentTestSession = testSession
            scope.launch {
                testSession.currentTestCardFlow.collect(currentTestCardStateFlow) // TODO: simplify this, try to use only testSession.currentTestCardFlow
                currentTestCardStateFlow.value = null // to propagate cancellation
            }
        }

        block(
            session.size,
            if (currentTestSession != null) {
                currentTestCardStateFlow.takeWhileNonNull()
            } else {
                null
            },
            currentCardStateFlow.takeWhileNonNull()
        )

        return session.results
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
            val session = LearningSession(sessionCards)
            currentSession = session

            scope.launch {
                session.currentCardFlow.collect(currentCardStateFlow)
                currentCardStateFlow.value = null // to propagate cancellation
            }
        }

        return currentSession
    }

    private fun prepareToNewCard() {
        hintStringStateFlow.value = emptyList()
        isWrongAnswerCounted = false
        checkCount = 0
        hintShowCountStateFlow.value = 0
    }

    suspend fun onCheckInput(answer: String): Boolean {
        ++checkCount

        val isRight = currentCard!!.term == answer
        if (isRight) {
            countRightAnswer()
            switchToNextCard()

        } else if (checkCount > 1) {
            countWrongAnswer()
        }

        return isRight
    }

    suspend fun onGiveUp() {
        val termChars = currentCard!!.term.map { it }
        if (hintStringStateFlow.value != termChars) {
            hintStringStateFlow.value = termChars
            countWrongAnswer()
        } else {
            switchToNextCard()
        }
    }

    suspend fun onHintAsked() {
        if (hintShowCountStateFlow.value > 1) {
            countWrongAnswer()
        } else {
            updateHintString()
            hintShowCountStateFlow.value++
        }
    }

    private fun updateHintString() {
        val term = currentCard!!.term
        val currentHint = if (hintStringStateFlow.value.isEmpty()) {
            term.map { HINT_HIDDEN_CHAR }
        } else {
            hintStringStateFlow.value
        }
        val indexToOpen = currentHint.indices.firstOrNull {
            currentHint[it] == HINT_HIDDEN_CHAR
        } ?: return

        hintStringStateFlow.value = currentHint.mapIndexed { index, c ->
            if (index == indexToOpen) {
                term[index]
            } else {
                c
            }
        }
    }

    private suspend fun countWrongAnswer() {
        if (isWrongAnswerCounted) return
        val currentCardSnapshot = currentCard!!

        val updatedCard = databaseWorker.runCancellable(
            currentCardSnapshot.id.toString(),
            runnable = {
                try {
                    database.cards.updateCard(
                        currentCardSnapshot.withWrongAnswer(timeSource)
                    )
                } catch (e: Throwable) {
                    // TODO: handle error
                    Logger.e("CardTeacher.countWrongAnswer", e.toString())
                    throw e
                }
            },
            delay = 0
        )

        currentSession!!.updateProgress(updatedCard, false)
        isWrongAnswerCounted = true
    }

    private suspend fun countRightAnswer() {
        val currentCardSnapshot = currentCard!!

        if (!isWrongAnswerCounted) {
            val updatedCard = databaseWorker.runCancellable(
                currentCardSnapshot.id.toString(),
                runnable = {
                    try {
                        database.cards.updateCard(
                            currentCardSnapshot.withRightAnswer(timeSource)
                        )
                    } catch (e: Throwable) {
                        // TODO: handle error
                        Logger.e("CardTeacher.countRightAnswer", e.toString())
                        throw e
                    }
                },
                delay = 0
            )

            currentSession!!.updateProgress(updatedCard, true)
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

    fun onTestOptionSelected(option: String): Boolean {
        return if (currentTestCard?.card?.term == option) {
            switchToNextTestCard()
            true
        } else {
            false
        }
    }

    private fun switchToNextTestCard(): TestSession.TestCard? {
        val nextTestCard = currentTestSession?.switchToNextCard()
        val safeTestSession = currentTestSession

        if (safeTestSession != null && nextTestCard == null) {
            testSessions.add(safeTestSession)
            currentTestSession = null
        }

        return nextTestCard
    }

    fun save() = State(
        currentTestSessionState = currentTestSession?.save(),
        sessionStates = sessions.map { it.save() },
        currentSessionState = currentSession?.save(),
        checkCount = checkCount,
        hintShowCount = hintShowCountStateFlow.value,
        isWrongAnswerCounted = isWrongAnswerCounted
    )

    fun restore(state: State) {
        currentTestSession = state.currentTestSessionState?.let(::createTestSession)
        sessions = state.sessionStates.map(::createLearningSession).toMutableList()
        currentSession = state.currentSessionState?.let(::createLearningSession)
        checkCount = state.checkCount
        hintShowCountStateFlow.value = state.hintShowCount
        isWrongAnswerCounted = state.isWrongAnswerCounted
    }

    private fun createLearningSession(sessionState: LearningSession.State) =
        LearningSession(
            cards = sessionState.cardIds.mapNotNull { cardId ->
                cards.firstOrNull { it.id == cardId }
            }
        ).also {
            it.restore(sessionState)
        }

    private fun createTestSession(sessionState: TestSession.State) =
        TestSession(
            cards = sessionState.cardIds.mapNotNull { cardId ->
                cards.firstOrNull { it.id == cardId }
            },
            options = cards.map { it.term }
        ).also {
            it.restore(sessionState)
        }

    @Parcelize
    data class State(
        val currentTestSessionState: TestSession.State?,
        val sessionStates: List<LearningSession.State>,
        val currentSessionState: LearningSession.State?,
        val checkCount: Int,
        val hintShowCount: Int,
        val isWrongAnswerCounted: Boolean
    ) : Parcelable
}

private const val CARD_PER_SESSION = 7
private const val HINT_HIDDEN_CHAR = '_'