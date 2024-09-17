package com.aglushkov.wordteacher.shared.repository.word_textsearch

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictWord
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.toOkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordTextSearchRepository(
    private val service: WordTeacherDictService
): SimpleResourceRepository<List<WordTeacherDictWord>, String>() {
    override suspend fun load(arg: String): List<WordTeacherDictWord> = withContext(Dispatchers.Default) {
        service.textSearch(arg).toOkResponse().words.orEmpty()
    }
}
