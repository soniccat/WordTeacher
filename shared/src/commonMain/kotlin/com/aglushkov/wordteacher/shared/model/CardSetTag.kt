package com.aglushkov.wordteacher.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CardSetTag (
    val name: String,
    val count: Int,
)

@Serializable
data class TagWithCardSet (
    val tag: CardSetTag,
    val cardSets: List<CardSet>,
)


fun String.toCardSetTag() = "#$this"