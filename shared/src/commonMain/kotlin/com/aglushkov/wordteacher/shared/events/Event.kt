package com.aglushkov.wordteacher.shared.events

interface Event {
    val isHandled: Boolean
        get() = false
    fun markAsHandled() {}
}