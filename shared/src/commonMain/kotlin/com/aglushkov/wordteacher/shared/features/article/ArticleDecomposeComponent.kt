package com.aglushkov.wordteacher.shared.features.article

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.article.ArticleRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume

class ArticleDecomposeComponent(
    componentContext: ComponentContext,
    id: Long,
    definitionsVM: DefinitionsVM,
    articleRepository: ArticleRepository,
    idGenerator: IdGenerator,
    router: ArticleRouter,
) : ArticleVMImpl (
    definitionsVM,
    articleRepository,
    ArticleVM.State(id = id, DefinitionsVM.State()), // TODO: it seems we can remove it
    router,
    idGenerator
), ComponentContext by componentContext {

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: ArticleVM.State(id = id, DefinitionsVM.State()))
    }

    init {
        stateKeeper.register(KEY_STATE) {
            state
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