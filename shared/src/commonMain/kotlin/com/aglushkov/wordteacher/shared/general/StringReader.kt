package com.aglushkov.wordteacher.shared.general

class StringReader(
    //private val keyCharacters: Set<Char>,
    private val escapeCharacter: Char = '\\'
) {
    var string: String = ""
        private set
    var pos: Int = 0
        private set
    val char: Char?
        get() {
            val ch = string.getOrNull(pos)
            return if (ch == escapeCharacter) {
                string.getOrNull(pos + 1)
            } else {
                ch
            }
        }
    val isCharEscaped: Boolean
        get() = string[pos] == escapeCharacter
    val nextChar: Char?
        get() {
            val ch = string.getOrNull(pos + 1)
            return if (ch == escapeCharacter) {
                string.getOrNull(pos + 2)
            } else {
                ch
            }
        }

    fun skip(count: Int = 1) {
        for (i in 0 until count) {
            if (!isEnd()) {
                readChar()
            } else {
                break;
            }
        }
    }

    fun read(s: String, reader: StringReader.() -> Unit) {
        string = s
        pos = 0
        reader()
    }

    fun isEnd() = pos >= string.length

    fun readChar(): Char {
        val ch = string[pos++]
        return if (ch == escapeCharacter) {
            string[pos++]
        } else {
            ch
        }
    }

    fun readUntil(stopChar: Char, needSkip: Boolean = false): Int {
        while (!isEnd() && char != stopChar) {
            readChar()
        }

        if (!isEnd() && needSkip) {
            skip()
        }

        return pos
    }

    fun readUntil(stopChar: Set<Char>): Int {
        while (!isEnd() && !stopChar.contains(char)) {
            readChar()
        }
        return pos
    }

    fun readWord() = readUntil(SPACE_CHAR_SET)
}

private val SPACE_CHAR_SET = setOf(' ', '\t')