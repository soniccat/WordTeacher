package com.aglushkov.wordteacher.shared.features.learning.vm

import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

class TestSession(
    var cards: List<Card>,
    options: List<String>
) {
    private var testCards: List<TestCard>
    private var currentIndex = 0

    private val currentTestCardStateFlow = MutableStateFlow<TestCard?>(null)
    val currentCard: TestCard?
        get() = currentTestCardStateFlow.value
    val currentTestCardFlow: Flow<TestCard>
        get() = currentTestCardStateFlow.takeWhileNonNull()

    val size: Int
        get() = testCards.size

    init {
        testCards = cards.map { card ->
            TestCard(
                card,
                (
                    options.filter {
                        it != card.term
                    }.shuffled().take(TEST_SESSION_OPTION_COUNT - 1) + card.term
                ).shuffled()
            )
        }

        currentTestCardStateFlow.value = testCards.getOrNull(currentIndex)
    }

    fun switchToNextCard(): TestCard? {
        var result: TestCard? = null
        ++currentIndex

        if (currentIndex < testCards.size) {
            result = testCards[currentIndex]
        }

        currentTestCardStateFlow.value = result
        return result
    }

    fun testCard(i: Int) =
        testCards[i]

    fun save() = State(
        cardIds = testCards.map { it.card.id },
        currentIndex = currentIndex,
        options = testCards.map { it.options }
    )

    fun restore(state: State) {
        currentIndex = state.currentIndex
        testCards = state.cardIds.zip(state.options)
            .map { pair ->
                TestCard(
                    cards.first { it.id == pair.first },
                    pair.second
                )
            }
        currentTestCardStateFlow.value = testCards[currentIndex]
    }

    fun deleteCard(id: Long) {
        val index = cards.indexOfFirst { it.id == id }
        if (index == -1) {
            return
        }

        if (index < currentIndex) {
            --currentIndex
        }
        cards = cards.filter { it.id != id }
        testCards = testCards.filter { it.card.id != id }

        currentTestCardStateFlow.value = testCards.getOrNull(currentIndex)
    }

    @Serializable
    data class State(
        val cardIds: List<Long>,
        val currentIndex: Int,
        val options: List<List<String>>
    )

    data class TestCard(
        val card : Card,
        val options: List<String>
    )
}

const val TEST_SESSION_OPTION_COUNT = 4