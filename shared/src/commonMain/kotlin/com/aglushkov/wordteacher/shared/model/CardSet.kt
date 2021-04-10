package com.aglushkov.wordteacher.shared.model

class CardSet (
    val id: Long,
    val name: String,
    val date: Long,
    var cards: List<Card> = emptyList()
) {

}