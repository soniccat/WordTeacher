package com.aglushkov.wordteacher.shared.features.article.di

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.Module
import dagger.Provides

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
        configuration: MainDecomposeComponent.ChildConfiguration.ArticleConfiguration,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        dictRepository: DictRepository,
        cardSetsRepository: CardSetsRepository,
        idGenerator: IdGenerator,
        nlpCore: NLPCore
    ): DefinitionsVM = DefinitionsVMImpl(
        DefinitionsVM.State(),
        connectivityManager,
        wordDefinitionRepository,
        dictRepository,
        cardSetsRepository,
        idGenerator
    )

    @Provides
    fun articleDecomposeComponent(
        componentContext: ComponentContext,
        configuration: MainDecomposeComponent.ChildConfiguration.ArticleConfiguration,
        definitionsVM: DefinitionsVM,
        articleRepository: ArticleRepository,
        cardsRepository: CardsRepository,
        dictRepository: DictRepository,
        idGenerator: IdGenerator,
        settings: FlowSettings
    ) = ArticleDecomposeComponent(
        componentContext,
        configuration.id,
        definitionsVM,
        articleRepository,
        cardsRepository,
        dictRepository,
        idGenerator,
        settings
    )
}
