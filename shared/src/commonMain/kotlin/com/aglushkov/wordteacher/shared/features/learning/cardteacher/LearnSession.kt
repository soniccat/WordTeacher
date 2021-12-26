package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.model.Card
import com.aglushkov.wordteacher.shared.model.MutableCard

class LearnSession(
    private val cards: List<MutableCard>
) {
    val results: List<SessionCardResult> = cards.map { card ->
        SessionCardResult(card.id, card.progress.progress())
    }

    private var currentIndex = 0

    val currentCard: MutableCard?
        get() = if (currentIndex < cards.size) cards[currentIndex] else null

    val size: Int
        get() = cards.size


    fun updateProgress(card: Card, isRight: Boolean) =
        getCardResult(card.id)?.let { result ->
            result.newProgress = card.progress.progress()
            result.isRight = isRight
        }

    fun nextCard(): Card? {
        var result: Card? = null
        ++currentIndex

        if (currentIndex < cards.size) {
            result = cards[currentIndex]
        }

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
