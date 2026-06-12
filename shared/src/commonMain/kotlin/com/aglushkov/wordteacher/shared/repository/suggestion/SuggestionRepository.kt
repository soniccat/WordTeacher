package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell

data class SuggestionResult(
    val fromDicts: List<Dict.Index.Entry>,
    val corrections: List<String>,
)

class SuggestionRepository(

): SimpleResourceRepository<SuggestionResult, String>() {

//    val checker = SymSpell(
//        dictionaryHolder = object : DictionaryHolder {
//
//        }
//    )

    override suspend fun loadInternal(arg: String): SuggestionResult {
        TODO("Not yet implemented")
    }
}