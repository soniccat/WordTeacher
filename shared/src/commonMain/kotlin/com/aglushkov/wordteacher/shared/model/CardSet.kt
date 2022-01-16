package com.aglushkov.wordteacher.shared.model

data class CardSet (
    val id: Long,
    val name: String,
    val date: Long,
    val cards: List<Card> = emptyList()
) {
    fun findCard(id: Long) =
        cards.firstOrNull { it.id == id }
}