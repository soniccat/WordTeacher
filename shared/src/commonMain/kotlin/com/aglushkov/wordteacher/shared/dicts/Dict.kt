package com.aglushkov.wordteacher.shared.dicts

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import okio.Path

interface Dict: WordTeacherWordService {
    val path: Path
    val name: String
    val fromLang: Language
    val toLang: Language

    suspend fun load()
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
