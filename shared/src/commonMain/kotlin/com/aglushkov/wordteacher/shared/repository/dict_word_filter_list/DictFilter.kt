package com.aglushkov.wordteacher.shared.repository.dict_word_filter_list

import okio.FileSystem
import okio.Path

class DictFilter(
    val path: Path,
    private val fileSystem: FileSystem
) {
    private var words = mutableListOf<String>()

    fun load() {

    }

    fun addWord(word: String) {

    }

    fun removeWord(word: String) {

    }
}