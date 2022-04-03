package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.okio.skipNewLine
import com.aglushkov.wordteacher.shared.general.okio.skipSpace
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

class DslIndex(
    private val dict: Dict,
    private val path: Path,
    private val fileSystem: FileSystem,
) : Dict.Index {
    private val index = DictTrie()

    init {
        if (fileSystem.exists(path)) {
            try {
                loadIndex()
            } catch (e: Throwable) {
                fileSystem.delete(path)
            }
        }
    }

    override fun allEntries(): Sequence<Dict.Index.Entry> {
        return index.asSequence()
    }

    override fun indexEntry(word: String): Dict.Index.Entry? {
        return index.word(word).firstOrNull()
    }

    override fun entry(
        word: String,
        nextWord: (needAnotherOne: Boolean) -> String?,
        onFound: (node: MutableList<Dict.Index.Entry>) -> Unit
    ) {
        return index.entry(word, nextWord, onFound)
    }

    private fun loadIndex() {
        fileSystem.read(path) {
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
                    add(it.word, it)
                }
            }
        }
    }

    fun add(term: String, partOfSpeech: WordTeacherWord.PartOfSpeech?, offset: Long) {
        add(
            term,
            Dict.Index.Entry(
                term,
                partOfSpeech ?: WordTeacherWord.PartOfSpeech.Undefined,
                offset,
                dict
            )
        )
    }

    fun add(term: String, entry: Dict.Index.Entry) =
        index.putWord(term, entry)

    fun save() {
        fileSystem.write(path) {
            writeIntValue(INDEX_VERSION, CURRENT_VERSION)

            index.onEach {
                writeEntry(it)
            }
        }
    }

    private fun BufferedSource.readEntry(): Dict.Index.Entry? {
        var word: String? = null
        var partOfSpeech: WordTeacherWord.PartOfSpeech? = null
        var offset = 0L

        var v: Int = -1
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
                INDEX_PART_OF_SPEECH -> {
                    partOfSpeech = WordTeacherWord.PartOfSpeech.values()[readInt()]
                    skipNewLine()
                }
                INDEX_OFFSET -> {
                    offset = readLong()
                    skipNewLine()
                }
            }
        }

        return if (word != null && partOfSpeech != null && offset != 0L) {
            Dict.Index.Entry(word, partOfSpeech, offset, dict)
        } else {
            null
        }
    }

    private fun BufferedSink.writeEntry(it: Dict.Index.Entry) {
        writeStringValue(INDEX_WORD, it.word)
        writeIntValue(INDEX_PART_OF_SPEECH, it.partOfSpeech.ordinal)
        writeLongValue(INDEX_OFFSET, it.indexValue as Long)

        writeInt(INDEX_WORD_END)
        writeUtf8("\n")
    }

    private fun BufferedSink.writeIntValue(key: Int, value: Int) {
        writeInt(key)
        writeUtf8(" ")
        writeInt(value)
        writeUtf8("\n")
    }

    private fun BufferedSink.writeLongValue(key: Int, value: Long) {
        writeInt(key)
        writeUtf8(" ")
        writeLong(value)
        writeUtf8("\n")
    }

    private fun BufferedSink.writeStringValue(key: Int, value: String) {
        writeInt(key)
        writeUtf8(" ")
        writeUtf8(value)
        writeUtf8("\n")
    }

    fun partOfSpeech(term: String) = index.word(term).firstOrNull()?.partOfSpeech

    fun offset(term: String): Long? = index.word(term).firstOrNull()?.indexValue as? Long

    fun isEmpty() = index.isEmpty()
}

class WrongVersionException: RuntimeException("")

private const val CURRENT_VERSION = 1

private const val INDEX_VERSION = 0
private const val INDEX_WORD = 1
private const val INDEX_PART_OF_SPEECH = 2
private const val INDEX_OFFSET = 3
private const val INDEX_WORD_END = 4
