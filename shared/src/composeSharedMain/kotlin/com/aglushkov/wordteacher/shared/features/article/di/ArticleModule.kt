package com.aglushkov.wordteacher.shared.features.article.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.Module
import dagger.Provides
import javax.sound.sampled.Clip

@Module
class ArticleModule {

    @Provides
    fun articleRepository(
        database: AppDatabase
    ) = ArticleRepository(database)

    @Provides
    fun cardsRepository(
        database: AppDatabase
    ) = CardsRepository(database)

    @Provides
    fun definitionsVM(
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        dictRepository: DictRepository,
        cardSetsRepository: CardSetsRepository,
        wordTeacherDictService: WordTeacherDictService,
        clipboardRepository: ClipboardRepository,
        idGenerator: IdGenerator,
        wordFrequencyGradationProvider: WordFrequencyGradationProvider,
        analytics: Analytics,
        settings: FlowSettings,
        wordDefinitionHistoryRepository: WordDefinitionHistoryRepository,
        audioService: AudioService,
    ): DefinitionsVM = DefinitionsVMImpl(
        DefinitionsVM.State(),
        connectivityManager,
        wordDefinitionRepository,
        dictRepository,
        cardSetsRepository,
        wordFrequencyGradationProvider,
        wordTeacherDictService,
        DefinitionsVM.Settings(
            needStoreDefinedWordInSettings = false,
        ),
        clipboardRepository,
        idGenerator,
        analytics,
        settings,
        wordDefinitionHistoryRepository,
        audioService,
    )

    @Provides
    fun articleDecomposeComponent(
        initialState: ArticleVM.State,
        componentContext: ComponentContext,
        definitionsVM: DefinitionsVM,
        articleRepository: ArticleRepository,
        articlesRepository: ArticlesRepository,
        cardsRepository: CardsRepository,
        dictRepository: DictRepository,
        idGenerator: IdGenerator,
        settings: FlowSettings,
        analytics: Analytics,
    ) = ArticleDecomposeComponent(
        componentContext,
        initialState,
        definitionsVM,
        articleRepository,
        articlesRepository,
        cardsRepository,
        dictRepository,
        idGenerator,
        settings,
        analytics,
    )
}
