package com.aglushkov.wordteacher.shared.model

import kotlinx.datetime.Instant

data class ShortCardSet (
    val id: Long,
    val name: String,
    val creationDate: Instant,
    val modificationDate: Instant,
    val readyToLearnProgress: Float,
    val totalProgress: Float,
    val creationId: String,
    val remoteId: String,
    val terms: List<String>,
) {

}