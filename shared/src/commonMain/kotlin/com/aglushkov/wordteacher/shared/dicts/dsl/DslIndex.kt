package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.DictTrieIndex
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
import java.util.Locale

class DslIndex(
    private val dict: Dict,
    override val path: Path,
    private val fileSystem: FileSystem,
) : DictTrieIndex {
    override val index = DictTrie()
    var wordCount = 0

    init {
        if (fileSystem.exists(path)) {
            try {
                loadIndex()
            } catch (e: Throwable) {
                fileSystem.delete(path)
            }
        }
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

            wordCount = 0
            while (!this.exhausted()) {
                readEntry()?.let {
                    add(it.word, it.partOfSpeeches, it.indexValue, it.dict)
                }
            }
        }
    }

    fun add(term: String, partOfSpeeches: List<WordTeacherWord.PartOfSpeech>, offset: Int) {
        add(
            term,
            partOfSpeeches,
            offset,
            dict
        )
    }

    fun add(
        term: String,
        partOfSpeeches: List<WordTeacherWord.PartOfSpeech>,
        indexValue: Any?,
        dict: Dict
    ) {
        index.put(term, DictWordData(partOfSpeeches, indexValue, dict))
        ++wordCount
    }

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
        val partOfSpeechesCount = readInt()
        val partOfSpeeches = mutableListOf<WordTeacherWord.PartOfSpeech>()
        for (i in 0 until partOfSpeechesCount) {
            partOfSpeeches.add(WordTeacherWord.PartOfSpeech.values()[readInt()])
        }

        val word = readUtf8Line()
        return if (word != null && offset != 0) {
            ReadEntry(word, partOfSpeeches, offset, dict)
        } else {
            null
        }
    }

    private fun BufferedSink.writeEntry(it: Dict.Index.Entry) {
        writeInt(it.indexValue as Int)
        writeInt(it.partOfSpeeches.size)
        it.partOfSpeeches.onEach {
            writeInt(it.ordinal)
        }
        writeUtf8(it.word)
        writeUtf8("\n")
    }

    fun partOfSpeech(term: String) = index.word(term).firstOrNull()?.partOfSpeeches

    fun offset(term: String): Int? = index.word(term).firstOrNull()?.indexValue as? Int

    fun isEmpty() = index.isEmpty()

    private data class ReadEntry(
        val word: String,
        val partOfSpeeches: List<WordTeacherWord.PartOfSpeech>,
        val indexValue: Any?,
        val dict: Dict
    )
}

class WrongVersionException: RuntimeException("")

private const val CURRENT_VERSION = 2

private const val INDEX_VERSION = 0
