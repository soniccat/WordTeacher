package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.Config
import com.aglushkov.wordteacher.shared.repository.ServiceMethodParams

interface WordTeacherWordService {
    val id: Int // TODO: parse id from a config
        get() {
            return 31 * baseUrl.hashCode() + 31 * key.hashCode()
        }

    var type: Config.Type
    var baseUrl: String
    var key: String
    var methodParams: ServiceMethodParams

    suspend fun define(word: String): List<WordTeacherWord>
}