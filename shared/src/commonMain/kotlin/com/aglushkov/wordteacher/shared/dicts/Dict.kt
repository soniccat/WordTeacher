package com.aglushkov.wordteacher.shared.dicts

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import okio.Path

interface Dict {
    val path: Path
    val name: String
    val fromLang: Language
    val toLang: Language

    suspend fun load()
    suspend fun define(term: String): WordTeacherWord?
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
