package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.sumOf
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardSet (
    @SerialName("_id")val id: Long?,
    @SerialName("id") val remoteId: String,
    val name: String,
    val creationDate: Instant,
    val modificationDate: Instant,
    val cards: List<Card> = emptyList(),
    val creationId: String,
) {
    fun requireId(): Long = id!!

    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }
}

fun List<Card>.totalProgress(): Float = calcProgress(this)

fun List<Card>.readyToLearnProgress(timeSource: TimeSource) =
    count { !it.progress.isReadyToLearn(timeSource) } / size.toFloat()

private fun calcProgress(aListOfCards: List<Card>): Float =
    aListOfCards.sumOf { it.progress.progress() } / aListOfCards.size.toFloat()

private fun CardSet.merge(anotherCardSet: CardSet, timeSource: TimeSource): CardSet {
    if (remoteId != anotherCardSet.remoteId) {
        throw RuntimeException("CardSet remoteId ($remoteId) != remoteCardSet remoteId (${anotherCardSet.remoteId})")
    }

    if (creationId != anotherCardSet.creationId) {
        throw RuntimeException("CardSet creationId ($creationId) != remoteCardSet creationId (${anotherCardSet.creationId})")
    }

    val oldCardSet: CardSet
    val newCardSet: CardSet
    if (modificationDate > anotherCardSet.modificationDate) {
        newCardSet = this
        oldCardSet = anotherCardSet
    } else {
        newCardSet = anotherCardSet
        oldCardSet = this
    }

    return CardSet(
        id = newCardSet.id,
        remoteId = newCardSet.remoteId,
        name = newCardSet.name,
        creationDate = newCardSet.creationDate,
        modificationDate = timeSource.getTimeInstant(),
        cards = oldCardSet.cards.mergeCards(newCardSet.cards),
        creationId = newCardSet.creationId,
    )
}
