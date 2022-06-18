package com.aglushkov.wordteacher.shared.model

data class ShortCardSet (
    val id: Long,
    val name: String,
    val date: Long,
    val readyToLearnProgress: Float,
    val totalProgress: Float
) {

}