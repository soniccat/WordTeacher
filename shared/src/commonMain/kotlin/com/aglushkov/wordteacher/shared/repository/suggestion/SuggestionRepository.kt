package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictWord
import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithFlow
import com.aglushkov.wordteacher.shared.general.toOkResponse
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.workers.FillMisspellingDBController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

data class SuggestionResult(
    val fromDicts: List<Dict.Index.Entry>,
    val corrections: List<String>,
    val texts: Resource<List<WordTeacherDictWord>>,
)

class SuggestionRepository(
    private val symSpellRepository: SymSpellRepository,
    private val dictRepository: DictRepository,
    private val wordTeacherDictService: WordTeacherDictService,
    private val wordFrequencyDatabase: WordFrequencyDatabase,
    private val misspellingDBController: FillMisspellingDBController,
): SimpleResourceRepository<SuggestionResult, String>() {

    val loadingMisspellingDBFlow: Flow<Resource<Unit>>
        get() = misspellingDBController.loadingFlow

    override suspend fun handleLoading(arg: String) {
        loadResourceWithFlow(
            flow = combine(
                flow = flow {
                    val entries = dictRepository.wordsStartWith(arg, 60)
                    if (entries.size < 30) {
                        emit(entries to Resource.Loading(null))
                        loadResource {
                            wordTeacherDictService.textSearch(arg).toOkResponse().words.orEmpty()
                        }.map {
                            entries to it
                        }.collect(this)
                    } else {
                        emit(entries to Resource.Uninitialized())
                    }
                },
                flow2 = loadCorrections(arg),
                transform = { (entries, textSearch), corrections ->
                    val entryTerms = entries.map { it.word }
                    val filteredCorrections = if (entries.isNotEmpty()) {
                        corrections.filter { !entryTerms.contains(it) }
                    } else {
                        corrections
                    }

                    SuggestionResult(entries, filteredCorrections, textSearch)
                }
            )
        ).collect(stateFlow)
    }

    fun loadCorrections(word: String): Flow<List<String>> = flow {
        val corrections = symSpellRepository.lookup(word)
        val frequency = wordFrequencyDatabase.resolveFrequencyForWords(corrections)

        emit(
            corrections.withIndex().sortedByDescending {
                frequency[it.index]
            }.map { it.value }
        )
    }

    // won't be called
    override suspend fun loadInternal(arg: String): SuggestionResult {
        return SuggestionResult(emptyList(), emptyList(), Resource.Uninitialized())
    }
}