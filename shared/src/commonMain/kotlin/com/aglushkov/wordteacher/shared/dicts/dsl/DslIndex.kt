package com.aglushkov.wordteacher.shared.dicts.dsl

import okio.FileSystem
import okio.Path

class DslIndex(
    private val path: Path,
    private val fileSystem: FileSystem
) {
    private val index = hashMapOf<String, Long>() //TODO: use trie

    init {
        if (fileSystem.exists(path)) {
            loadIndex()
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
