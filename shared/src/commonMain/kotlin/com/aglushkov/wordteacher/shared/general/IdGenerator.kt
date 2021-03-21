package com.aglushkov.wordteacher.shared.general

class IdGenerator {
    var id = 0L

    fun nextId() = ++id
}