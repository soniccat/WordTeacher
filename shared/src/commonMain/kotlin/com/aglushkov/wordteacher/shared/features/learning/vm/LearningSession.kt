package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LearningSession(
    private var cards: List<Card>
) {
    var results: List<SessionCardResult> = cards.map { card ->
        SessionCardResult(card.id, card.progress.progress())
    }
        private set

    private var currentIndex = 0

    private val currentCardStateFlow = MutableStateFlow<Card?>(null)
    val currentCard: Card?
        get() = currentCardStateFlow.value
    val currentCardFlow: Flow<Card>
        get() = currentCardStateFlow.takeWhileNonNull()

    val size: Int
        get() = cards.size

    init {
        currentCardStateFlow.value = cards.getOrNull(currentIndex)
    }

    fun updateProgress(card: Card, isRight: Boolean) =
        updateCardResult(card) { result ->
            result.copy(
                newProgress = card.progress.progress(),
                isRight = isRight
            )
        }

    fun switchToNextCard(): Card? {
        var result: Card? = null
        ++currentIndex

        if (currentIndex < cards.size) {
            result = cards[currentIndex]
        }

        currentCardStateFlow.value = result
        return result
    }

    private fun updateCardResult(card: Card, transform: (SessionCardResult) -> SessionCardResult) {
        cards = cards.map { if (it.id == card.id) card else it }
        results = results.map { sessionCardResult ->
            if (sessionCardResult.cardId == card.id) {
                transform(sessionCardResult)
            } else {
                sessionCardResult
            }
        }
    }

    fun rightAnsweredCards() =
        cards.filterIndexed { index, card ->
            results[index].isRight
        }

    fun card(i: Int) =
        cards[i]

    fun cardResult(i: Int) =
        results[i]

    fun save() = State(
        cardIds = cards.map { it.id },
        currentIndex = currentIndex,
        results = results
    )

    fun restore(state: State) {
        currentIndex = state.currentIndex
        results = state.results
        currentCardStateFlow.value = cards[currentIndex]
    }

    @Parcelize
    data class State(
        val cardIds: List<Long>,
        val currentIndex: Int,
        val results: List<SessionCardResult>
    ) : Parcelable
}
