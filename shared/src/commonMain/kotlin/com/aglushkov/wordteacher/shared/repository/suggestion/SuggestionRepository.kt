package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictWord
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlinx.coroutines.flow.channelFlow

data class SuggestionResult(
    val fromDicts: List<Dict.Index.Entry>,
    val corrections: List<String>,
    val texts: List<WordTeacherDictWord>,
)

class SuggestionRepository(
    private val symSpellRepository: SymSpellRepository,
): SimpleResourceRepository<SuggestionResult, String>() {

    override suspend fun handleLoading(arg: String) {
        loadResourceWithProgress(
            loader = channelFlow {
                val r = symSpellRepository.lookup(arg)

                send(1.0f to SuggestionResult(emptyList(), r, emptyList()))
            }
        ).collect(stateFlow)
    }

    // won't be called
    override suspend fun loadInternal(arg: String): SuggestionResult {
        return SuggestionResult(emptyList(), emptyList(), emptyList())
    }
}