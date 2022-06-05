package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.extensions.writeIntValue
import com.aglushkov.wordteacher.shared.general.extensions.writeLongValue
import com.aglushkov.wordteacher.shared.general.extensions.writeStringValue
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
    private val index = HashMap<String, Long>()

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
                    add(it.word,it.offset)
                }
            }
        }
    }

    fun add(word: String, offset: Long) {
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
        var offset = 0L

        var v: Int
        while(!exhausted()) {
            v = readInt()
            if (v == INDEX_WORD_END) {
                if (!exhausted()) {
                    skipNewLine()
                }
                break
            }
            skipSpace()

            when (v) {
                INDEX_WORD -> word = readUtf8Line()
                INDEX_OFFSET -> {
                    offset = readLong()
                    skipNewLine()
                }
            }
        }

        return if (word != null && offset != 0L) {
            ReadEntry(word, offset)
        } else {
            null
        }
    }

    private fun BufferedSink.writeEntry(it: Map.Entry<String, Long>) {
        writeStringValue(INDEX_WORD, it.key)
        writeLongValue(INDEX_OFFSET, it.value)

        writeInt(INDEX_WORD_END)
        writeUtf8("\n")
    }

    fun offset(term: String): Long? = index[term]

    fun isEmpty() = index.isEmpty()

    private data class ReadEntry(
        val word: String,
        val offset: Long
    )
}

class WrongVersionException: RuntimeException("")

private const val CURRENT_VERSION = 1

private const val INDEX_VERSION = 0
private const val INDEX_WORD = 1
private const val INDEX_OFFSET = 2
private const val INDEX_WORD_END = 3
