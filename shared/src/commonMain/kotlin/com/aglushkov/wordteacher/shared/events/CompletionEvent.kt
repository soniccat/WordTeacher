package com.aglushkov.wordteacher.shared.events

enum class CompletionResult {
    COMPLETED,
    CANCELLED
}

sealed interface CompletionData {
    data class Article(val id: Long): CompletionData
}

data class CompletionEvent(val result: CompletionResult, val data: CompletionData? = null) : Event
