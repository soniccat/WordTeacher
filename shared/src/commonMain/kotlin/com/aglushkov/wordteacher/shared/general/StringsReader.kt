package com.aglushkov.wordteacher.shared.general

import kotlin.math.min

// sth like https://pkg.go.dev/strings#NewReader
class StringsReader(
    val str: String,
) {
    var pos: Int = 0
        private set

    val currentChar: Char
        get() = str[pos]

    fun atEnd(): Boolean =
        pos >= str.length

    fun readChar(): Char {
        return str[pos].also {
            ++pos
        }
    }

    fun readUntil(char: Char): Boolean {
        while (!atEnd()) {
            val newChar = readChar()
            if (newChar == char) {
                return true
            }
        }

        return false
    }

    fun peek(l: Int): String {
        val endIndex = min(str.length, pos + l)
        return str.substring(pos, endIndex)
    }

    fun seek(l: Int) {
        pos = min(str.length, pos + l)
    }

    fun substring(start: Int, end: Int) = str.substring(start, end)
}
