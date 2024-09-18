package com.aglushkov.wordteacher.shared.features.definitions.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.word_textsearch.WordTextSearchRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class DefinitionsModule {
    @Provides
    fun definitionsDecomposeComponent(
        componentContext: ComponentContext,
        initialState: DefinitionsVM.State,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        dictRepository: DictRepository,
        cardSetsRepository: CardSetsRepository,
        wordFrequencyGradationProvider: WordFrequencyGradationProvider,
        wordTeacherDictService: WordTeacherDictService,
        idGenerator: IdGenerator,
        analytics: Analytics,
    ) = DefinitionsDecomposeComponent(
        componentContext,
        initialState,
        connectivityManager,
        wordDefinitionRepository,
        dictRepository,
        cardSetsRepository,
        wordFrequencyGradationProvider,
        wordTeacherDictService,
        idGenerator,
        analytics,
    )
}
