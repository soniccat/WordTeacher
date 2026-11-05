package com.aglushkov.wordteacher.shared.repository.remote_dict

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDict
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.toOkResponse

class RemoteDictRepository(
    private val dictService: WordTeacherDictService
): SimpleResourceRepository<List<WordTeacherDict>, Unit>() {

    override suspend fun loadInternal(arg: Unit): List<WordTeacherDict> {
        return dictService.files().toOkResponse().let {
            it.dicts.orEmpty()
        }
    }

    fun loadDict(dict: WordTeacherDict) {
        // TODO:
    }
}

