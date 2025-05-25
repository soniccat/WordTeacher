package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

class CardTeacher(
    private val cards: List<Card>,
    private val databaseCardWorker: DatabaseCardWorker,
    private val timeSource: TimeSource,
    private val scope: CoroutineScope
) {
    private var testSessions = mutableListOf<TestSession>()
    private var currentTestSession: TestSession? = null
    private var currentTestCardStateFlow = MutableStateFlow<TestSession.TestCard?>(null)
    private val currentTestCard: TestSession.TestCard?
        get() = currentTestCardStateFlow.value

    private var matchSession: MatchSession? = null

    private var sessions = mutableListOf<LearningSession>()
    private var currentSession: LearningSession? = null
    private var currentCardStateFlow = MutableStateFlow<Card?>(null)
    val currentCard: Card?
        get() = currentCardStateFlow.value

    private var hintStringStateFlow = MutableStateFlow<List<Char>>(emptyList())
    private var hintShowCountStateFlow = MutableStateFlow(0)

    val hintString: Flow<List<Char>>
        get() = hintStringStateFlow
    val hintShowCount: Flow<Int>
        get() = hintShowCountStateFlow

    private var checkCount = 0
    private var isWrongAnswerCounted = false
        private set

    suspend fun runSession(
        block: suspend (count:Int, matchSessionFlow:  Flow<List<MatchSession.MatchPair>>?, testCards: Flow<TestSession.TestCard>?, cards: Flow<Card>) -> Unit
    ): List<SessionCardResult>? {
        val session = buildLearnSession() ?: return null
        val warmupSession = Random.nextInt(2)

        if (MatchSession.isValid(session.cards) && warmupSession == 0 && session.cards.size >= MATCH_SESSION_OPTION_COUNT) {
            matchSession = MatchSession(session.cards)
        }

        if ((matchSession == null || warmupSession == 1) && session.cards.size >= TEST_SESSION_OPTION_COUNT) {
            val testSession = TestSession(session.cards, cards.map { it.term })
            currentTestSession = testSession
            scope.launch {
                testSession.currentTestCardFlow.collect(currentTestCardStateFlow) // TODO: simplify this, try to use only testSession.currentTestCardFlow
                currentTestCardStateFlow.value = null // to propagate cancellation
            }
        }

        block(
            session.size,
            matchSession?.matchPairFlow?.takeWhile { it != null } as? Flow<List<MatchSession.MatchPair>>,
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
        }.sortedByFrequency().take(CARD_PER_SESSION)

        if (sessionCards.isEmpty()) {
            currentSession = null
        } else {
            val session = LearningSession(sessionCards.shuffled())
            currentSession = session

            scope.launch {
                session.currentCardFlow.collect(currentCardStateFlow)
                currentCardStateFlow.value = null // to propagate cancellation
            }
        }

        return currentSession
    }

    private fun List<Card>.sortedByFrequency(): List<Card> {
        val cardsWithNegativeFrequency = mutableListOf<Card>()
        val cardsWithPositiveFrequency = mutableListOf<Card>()
        onEach {
            if (it.termFrequency < 0) {
                cardsWithNegativeFrequency.add(it)
            } else {
                cardsWithPositiveFrequency.add(it)
            }
        }

        return cardsWithPositiveFrequency.sortedByDescending { it.termFrequency } + cardsWithNegativeFrequency
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
            if (!isWrongAnswerCounted) {
                countRightAnswer()
            }
            switchToNextCard()

        } else if (checkCount > 1) { // TODO: move to settings
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
        if (hintShowCountStateFlow.value > 1) { // TODO: move to settings
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

    suspend fun countWrongAnswer() {
        if (isWrongAnswerCounted) return

        val updatedCard = databaseCardWorker.updateCardAndWait(currentCard!!.withWrongAnswer(timeSource), timeSource.timeInMilliseconds())
        currentSession!!.updateProgress(updatedCard, false)
        isWrongAnswerCounted = true
    }

    private suspend fun countRightAnswer() {
        val updatedCard = databaseCardWorker.updateCardAndWait(currentCard!!.withRightAnswer(timeSource), timeSource.timeInMilliseconds())
        currentSession!!.updateProgress(updatedCard, true)
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

    fun onMatchTermSelected(matchPair: MatchSession.MatchPair) {
        matchSession?.selectTerm(matchPair)
    }

    fun onMatchExampleSelected(matchPair: MatchSession.MatchPair) {
        matchSession?.selectExample(matchPair)
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

    @Serializable
    data class State(
        val currentTestSessionState: TestSession.State?,
        val sessionStates: List<LearningSession.State>,
        val currentSessionState: LearningSession.State?,
        val checkCount: Int,
        val hintShowCount: Int,
        val isWrongAnswerCounted: Boolean
    )
}

private const val CARD_PER_SESSION = 7
private const val HINT_HIDDEN_CHAR = '_'