package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.extensions.writeIntValue
import com.aglushkov.wordteacher.shared.general.okio.skipNewLine
import com.aglushkov.wordteacher.shared.general.okio.skipSpace
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
import com.aglushkov.wordteacher.shared.repository.dict.DictWordData
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

class DslIndex(
    private val dict: Dict,
    override val path: Path,
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

    override fun entriesStartWith(prefix: String, limit: Int): List<Dict.Index.Entry> =
        index.wordsStartWith(prefix, limit)

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
                    add(it.word, it.partOfSpeech, it.indexValue, it.dict)
                }
            }
        }
    }

    fun add(term: String, partOfSpeech: WordTeacherWord.PartOfSpeech?, offset: Int) {
        add(
            term,
            partOfSpeech ?: WordTeacherWord.PartOfSpeech.Undefined,
            offset,
            dict
        )
    }

    fun add(
        term: String,
        partOfSpeech: WordTeacherWord.PartOfSpeech,
        indexValue: Any?,
        dict: Dict
    ) =
        index.put(term, DictWordData(partOfSpeech, indexValue, dict))

    fun save() {
        fileSystem.write(path) {
            writeIntValue(INDEX_VERSION, CURRENT_VERSION)

            index.onEach {
                writeEntry(it)
            }
        }
    }

    private fun BufferedSource.readEntry(): ReadEntry? {
        val offset = readInt()
        val partOfSpeech = WordTeacherWord.PartOfSpeech.values()[readInt()]
        val word = readUtf8Line()

        return if (word != null && offset != 0) {
            ReadEntry(word, partOfSpeech, offset, dict)
        } else {
            null
        }
    }

    private fun BufferedSink.writeEntry(it: Dict.Index.Entry) {
        writeInt(it.indexValue as Int)
        writeInt(it.partOfSpeech.ordinal)
        writeUtf8(it.word)
        writeUtf8("\n")
    }

    fun partOfSpeech(term: String) = index.word(term).firstOrNull()?.partOfSpeech

    fun offset(term: String): Int? = index.word(term).firstOrNull()?.indexValue as? Int

    fun isEmpty() = index.isEmpty()

    private data class ReadEntry(
        val word: String,
        val partOfSpeech: WordTeacherWord.PartOfSpeech,
        val indexValue: Any?,
        val dict: Dict
    )
}

class WrongVersionException: RuntimeException("")

private const val CURRENT_VERSION = 2

private const val INDEX_VERSION = 0
