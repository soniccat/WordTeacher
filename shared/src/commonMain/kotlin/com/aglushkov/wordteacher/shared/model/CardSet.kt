package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.sumOf

data class CardSet (
    val id: Long,
    val name: String,
    val date: Long,
    val cards: List<Card> = emptyList()
) {
    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }
}

fun List<Card>.totalProgress(): Float = calcProgress(this)

fun List<Card>.readyToLearnProgress(timeSource: TimeSource) =
    calcProgress(
        filter { it.progress.isReadyToLearn(timeSource) }
    )

private fun calcProgress(aListOfCards: List<Card>): Float =
    aListOfCards.sumOf { it.progress.progress() } / aListOfCards.size.toFloat()