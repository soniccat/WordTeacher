package com.aglushkov.wordteacher.shared.general

class IdGenerator {
    private var id = 0L

    fun nextId() = ++id
}