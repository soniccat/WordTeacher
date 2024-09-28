package com.aglushkov.wordteacher.shared.service

import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams

interface WordTeacherWordService {
    var type: Config.Type

    suspend fun define(words: List<String>): List<WordTeacherWord>
}
