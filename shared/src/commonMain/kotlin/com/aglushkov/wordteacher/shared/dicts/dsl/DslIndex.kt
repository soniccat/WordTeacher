package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import okio.FileSystem
import okio.Path

class DslIndex(
    private val dict: Dict,
    private val path: Path,
    private val fileSystem: FileSystem,
) : Dict.Index {
    private val index = hashMapOf<String, Long>() //TODO: use trie

    init {
        if (fileSystem.exists(path)) {
            loadIndex()
        }
    }

    override fun allEntries(): Sequence<Dict.Index.Entry> {
        return index.asSequence().map { Dict.Index.Entry(it.key, it.value, dict) }
    }

    override fun indexEntry(word: String): Dict.Index.Entry? {
        return index[word]?.let { offset ->
            Dict.Index.Entry(word, offset, dict)
        }
    }

    private fun loadIndex() {
        fileSystem.read(path) {
            while (!this.exhausted()) {
                val key = readUtf8Line()
                val value = readUtf8Line()

                if (key != null && value != null) {
                    value.toLongOrNull()?.let { offset ->
                        index[key] = offset
                    }
                }
            }
        }
    }

    fun add(term: String, offset: Long) {
        index[term] = offset
    }

    fun save() {
        fileSystem.write(path) {
            index.onEach {
                writeUtf8(it.key)
                writeUtf8("\n")
                writeUtf8(it.value.toString())
                writeUtf8("\n")
            }
        }
    }

    fun get(term: String): Long? = index[term]

    fun isEmpty() = index.isEmpty()
}
