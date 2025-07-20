package com.aglushkov.wordteacher.shared.features.article

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent.Companion
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume

class ArticleDecomposeComponent(
    componentContext: ComponentContext,
    initialState: ArticleVM.State,
    definitionsVM: DefinitionsVM,
    articleRepository: ArticleRepository,
    articlesRepository: ArticlesRepository,
    cardsRepository: CardsRepository,
    dictRepository: DictRepository,
    idGenerator: IdGenerator,
    settings: SettingStore,
    analytics: Analytics,
) : ArticleVMImpl (
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = ArticleVM.State.serializer()
    ) ?: initialState,
    definitionsVM,
    articleRepository,
    articlesRepository,
    cardsRepository,
    dictRepository,
    idGenerator,
    settings,
    analytics,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Article"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = ArticleVM.State.serializer()
        ) {
            state.value.toState()
        }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}