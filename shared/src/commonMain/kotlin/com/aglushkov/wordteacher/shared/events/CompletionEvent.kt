package com.aglushkov.wordteacher.shared.events

enum class CompletionResult {
    COMPLETED,
    CANCELLED
}

data class CompletionEvent(val result: CompletionResult) : Event