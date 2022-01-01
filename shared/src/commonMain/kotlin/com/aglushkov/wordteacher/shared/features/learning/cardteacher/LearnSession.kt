package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.general.extensions.asCommonFlow
import com.aglushkov.wordteacher.shared.general.extensions.takeWhileNonNull
import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

class LearnSession(
    private val cards: List<MutableCard>
) {
    val results: List<SessionCardResult> = cards.map { card ->
        SessionCardResult(card.id, card.progress.progress())
    }

    private var currentIndex = 0

    private val currentCardStateFlow = MutableStateFlow<MutableCard?>(null)
    val currentCard: MutableCard?
        get() = currentCardStateFlow.value
    val currentCardFlow: Flow<MutableCard>
        get() = currentCardStateFlow.takeWhileNonNull()

    val size: Int
        get() = cards.size


    fun updateProgress(card: Card, isRight: Boolean) =
        getCardResult(card.id)?.let { result ->
            result.newProgress = card.progress.progress()
            result.isRight = isRight
        }

    fun switchToNextCard(): Card? {
        var result: MutableCard? = null
        ++currentIndex

        if (currentIndex < cards.size) {
            result = cards[currentIndex]
        }

        currentCardStateFlow.value = result
        return result
    }

    private fun getCardResult(cardId: Long) =
        results.firstOrNull { it.cardId == cardId }

    fun rightAnsweredCards() =
        cards.filterIndexed { index, card ->
            results[index].isRight
        }

    fun card(i: Int) =
        cards[i]

    fun cardResult(i: Int) =
        results[i]
}
