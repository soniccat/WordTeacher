package com.aglushkov.wordteacher.shared.dicts

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import okio.Path

interface Dict: WordTeacherWordService {
    val path: Path
    val name: String
    val fromLang: Language
    val toLang: Language
    val index: Index

    suspend fun load()
    suspend fun define(word: String, indexEntry: Index.Entry): List<WordTeacherWord>

    interface Index {
        fun allEntries(): Sequence<Entry>
        fun indexEntry(word: String): Entry?

        data class Entry(val word: String, val indexValue: Any?, val dict: Dict)
    }
}

enum class Language {
    RU,
    EN,
    UNKNOWN;

    companion object {
        fun parse(string: String): Language {
            return when(string.lowercase()) {
                "ru", "russian" -> RU
                "en", "english" -> EN
                else -> UNKNOWN
            }
        }
    }
}
