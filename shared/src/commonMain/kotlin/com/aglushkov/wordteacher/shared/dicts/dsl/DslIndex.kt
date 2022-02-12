package com.aglushkov.wordteacher.shared.dicts.dsl

import okio.FileSystem
import okio.Path

class DslIndex(
    private val path: Path,
    private val fileSystem: FileSystem
) {
    private val index = hashMapOf<String, Long>() //TODO: use trie

    init {
        // TODO: load index
    }

    fun add(term: String, offset: Long) {
        index.put(term, offset)
    }

    fun save() {
        // TODO: save index
    }

    fun get(term: String): Long? = index[term]

    fun isEmpty() = index.isEmpty()
}
