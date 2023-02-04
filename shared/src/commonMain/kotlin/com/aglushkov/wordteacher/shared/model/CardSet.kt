package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.sumOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

data class CardSet (
    val id: Long,
    val name: String,
    val creationDate: Instant,
    val modificationDate: Instant,
    val cards: List<Card> = emptyList(),
    val creationId: String,
) {
    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }
}

fun List<Card>.totalProgress(): Float = calcProgress(this)

fun List<Card>.readyToLearnProgress(timeSource: TimeSource) =
    count { !it.progress.isReadyToLearn(timeSource) } / size.toFloat()

private fun calcProgress(aListOfCards: List<Card>): Float =
    aListOfCards.sumOf { it.progress.progress() } / aListOfCards.size.toFloat()
