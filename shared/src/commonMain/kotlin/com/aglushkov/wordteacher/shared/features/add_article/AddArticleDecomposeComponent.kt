package com.aglushkov.wordteacher.shared.features.add_article

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVMImpl
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume

class AddArticleDecomposeComponent(
    componentContext: ComponentContext,
    articlesRepository: ArticlesRepository,
    contentExtractors: Array<ArticleContentExtractor>,
    cardSetsRepository: CardSetsRepository,
    timeSource: TimeSource,
    analytics: Analytics,
    private val initialState: AddArticleVM.State = AddArticleVM.State(),
): AddArticleVMImpl(
    articlesRepository,
    contentExtractors,
    cardSetsRepository,
    timeSource,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "AddArticle"

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: initialState)
    }

    init {
        baseInit(analytics)

        stateKeeper.register(KEY_STATE) {
            createState()
        }

        restore(instanceState.state)
    }

    private class Handler(val state: AddArticleVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}