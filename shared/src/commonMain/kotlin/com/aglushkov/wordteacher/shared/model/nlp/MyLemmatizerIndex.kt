package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.writeIntValue
import com.aglushkov.wordteacher.shared.general.okio.skipNewLine
import com.aglushkov.wordteacher.shared.general.okio.skipSpace
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

class MyLemmatizerIndex(
    private val indexPath: Path,
    private val fileSystem: FileSystem,
) {
    private val index = HashMap<String, Int>()

    init {
        if (fileSystem.exists(indexPath)) {
            try {
                loadIndex()
            } catch (e: Throwable) {
                fileSystem.delete(indexPath)
            }
        }
    }

    private fun loadIndex() {
        fileSystem.read(indexPath) {
            val v = readInt()
            if (v == INDEX_VERSION) {
                skipSpace()
                val version = readInt()
                if (version != CURRENT_VERSION) {
                    throw WrongVersionException()
                }
            }
            skipNewLine()

            while (!this.exhausted()) {
                readEntry()?.let {
                    add(it.word, it.offset)
                }
            }
        }
    }

    fun add(word: String, offset: Int) {
        index[word] = offset
    }

    fun save() {
        fileSystem.write(indexPath) {
            writeIntValue(INDEX_VERSION, CURRENT_VERSION)

            index.onEach {
                writeEntry(it)
            }
        }
    }

    private fun BufferedSource.readEntry(): ReadEntry? {
        var word: String? = null
        var offset = 0

        try {
            offset = readInt()
            word = readUtf8Line()
        } catch (e: Exception) {
            Logger.e(e.message.orEmpty())
        }

        return if (word != null && offset != 0) {
            ReadEntry(word, offset)
        } else {
            null
        }
    }

    private fun BufferedSink.writeEntry(it: Map.Entry<String, Int>) {
        writeInt(it.value)
        writeUtf8(it.key)
        writeUtf8("\n")
    }

    fun offset(term: String): Int? = index[term]

    fun isEmpty() = index.isEmpty()

    private data class ReadEntry(
        val word: String,
        val offset: Int
    )
}

class WrongVersionException: RuntimeException("")

private const val CURRENT_VERSION = 2
private const val INDEX_VERSION = 0

