package com.aglushkov.wordteacher.shared.model

interface CardSet {
    val id: Long
    val name: String
    val date: Long
    val cards: List<Card>

    fun toMutableCardSet() =
        MutableCardSet(
            id = id,
            name = name,
            date = date,
            cards = cards.toMutableList().map { it.toMutableCard() }
        )

    fun toImmutableCardSet() =
        ImmutableCardSet(
            id = id,
            name = name,
            date = date,
            cards = cards.toList().map { it.toImmutableCard() }
        )

    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }
}

data class ImmutableCardSet (
    override val id: Long,
    override val name: String,
    override val date: Long,
    override val cards: List<Card> = emptyList()
) : CardSet {

}

data class MutableCardSet (
    override var id: Long,
    override var name: String,
    override var date: Long,
    override var cards: List<MutableCard> = emptyList()
) : CardSet {
    override fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }

    fun removeCard(card: Card) {
        cards = cards.filter { it.id != card.id }
    }

    fun addCard(card: MutableCard) {
        cards = cards + card
    }
}
