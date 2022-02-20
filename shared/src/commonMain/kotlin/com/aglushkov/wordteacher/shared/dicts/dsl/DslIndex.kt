package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.repository.dict.DictTrie
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
            loadIndex()
        }
    }

    override fun allEntries(): Sequence<Dict.Index.Entry> {
        return index.asSequence()
    }

    override fun indexEntry(word: String): Dict.Index.Entry? {
        return index.word(word).firstOrNull()
    }

    private fun loadIndex() {
        fileSystem.read(path) {
            while (!this.exhausted()) {
                val key = readUtf8Line()
                val value = readUtf8Line()

                if (key != null && value != null) {
                    value.toLongOrNull()?.let { offset ->
                        add(key, offset)
                    }
                }
            }
        }
    }

    fun add(term: String, offset: Long) {
        index.putWord(term, Dict.Index.Entry(term, offset, dict))
    }

    fun save() {
        fileSystem.write(path) {
            index.onEach {
                writeUtf8(it.word)
                writeUtf8("\n")
                writeUtf8((it.indexValue as Long).toString())
                writeUtf8("\n")
            }
        }
    }

    fun get(term: String): Long? = index.word(term).firstOrNull()?.indexValue as? Long

    fun isEmpty() = index.isEmpty()
}
