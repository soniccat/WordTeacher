package com.aglushkov.wordteacher.shared.model

data class ShortCardSet (
    val id: Long,
    val name: String,
    val creationDate: Long,
    val modificationDate: Long,
    val readyToLearnProgress: Float,
    val totalProgress: Float,
) {

}