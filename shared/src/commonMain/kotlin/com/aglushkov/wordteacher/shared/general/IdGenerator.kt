package com.aglushkov.wordteacher.shared.general

class IdGenerator {
    var id = 0

    fun nextId() = ++id
}