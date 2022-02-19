package com.aglushkov.wordteacher.androidApp.features.article.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class ArticleComposeModule {

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
        idGenerator: IdGenerator,
        router: RouterResolver
    ) = ArticleDecomposeComponent(
        componentContext,
        configuration.id,
        definitionsVM,
        articleRepository,
        cardsRepository,
        idGenerator,
        router.router!!.get()!!
    )
}
