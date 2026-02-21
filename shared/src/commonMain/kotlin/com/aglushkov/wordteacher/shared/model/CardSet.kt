package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.extensions.sumOf
import com.aglushkov.wordteacher.shared.general.serialization.InstantIso8601Serializer
import com.aglushkov.wordteacher.shared.repository.cardset.CardEnricher
import com.benasher44.uuid.uuid4
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.ExperimentalTime

@Serializable
data class CardSet (
    @Transient val id: Long = 0,
    @SerialName("id") val remoteId: String,
    val name: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val creationDate: Instant,
    @Serializable(with = InstantIso8601Serializer::class)
    val modificationDate: Instant,
    @SerialName("cards") val cards: List<Card> = emptyList(),
    var terms: List<String> = emptyList(), // for cardsets from search
    val creationId: String,
    val info: CardSetInfo = CardSetInfo("", null),
    val isAvailableInSearch: Boolean = false,
    val tags: List<String> = emptyList()
) {
    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }

    fun copyWithDate(creationDate: Instant) =
        this.copy(
            creationDate = creationDate,
            modificationDate = creationDate,
            creationId = uuid4().toString(),
            remoteId = "",
            cards = cards.map {
                it.copy(
                    creationDate = creationDate,
                    modificationDate = creationDate,
                    creationId = uuid4().toString(),
                    remoteId = "",
                )
            },
        )
    }

fun List<Card>.totalProgress(): Float = calcProgress(this)

fun List<Card>.readyToLearnProgress(timeSource: TimeSource) =
    count { !it.progress.isReadyToLearn(timeSource) } / size.toFloat()

private fun calcProgress(aListOfCards: List<Card>): Float =
    aListOfCards.sumOf { it.progress.progress() } / aListOfCards.size.toFloat()

fun CardSet.merge(anotherCardSet: CardSet, newModificationDate: Instant): CardSet {
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
        modificationDate = newModificationDate,
        cards = oldCardSet.cards.mergeCards(newCardSet.cards),
        creationId = newCardSet.creationId,
        info = newCardSet.info,
        isAvailableInSearch = newCardSet.isAvailableInSearch,
    )
}

@Serializable
data class CardSetInfo(
    val description: String,
    val source:      String?,
)
