package com.aglushkov.wordteacher.shared.features.article

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume
import com.russhwolf.settings.coroutines.FlowSettings

class ArticleDecomposeComponent(
    componentContext: ComponentContext,
    id: Long,
    definitionsVM: DefinitionsVM,
    articleRepository: ArticleRepository,
    cardsRepository: CardsRepository,
    dictRepository: DictRepository,
    idGenerator: IdGenerator,
    settings: FlowSettings,
    analytics: Analytics,
) : ArticleVMImpl (
    definitionsVM,
    articleRepository,
    cardsRepository,
    dictRepository,
    id,
    idGenerator,
    settings,
    analytics,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Article"

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: ArticleVM.State(id = id))
    }

    init {
        baseInit(analytics)

        stateKeeper.register(KEY_STATE) {
            state.value.toState()
        }

        restore(instanceState.state)
    }

    private class Handler(val state: ArticleVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}