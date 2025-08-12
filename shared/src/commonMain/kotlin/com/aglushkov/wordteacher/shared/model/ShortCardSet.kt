package com.aglushkov.wordteacher.shared.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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