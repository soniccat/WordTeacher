package com.aglushkov.wordteacher.shared.features.add_article

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVMImpl
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent.Companion
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
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
    private val initialState: AddArticleVM.State,
): AddArticleVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = AddArticleVM.State.serializer()
    ) ?: initialState,
    articlesRepository,
    contentExtractors,
    cardSetsRepository,
    timeSource,
    analytics,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_AddArticle"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = AddArticleVM.State.serializer()
        ) {
            createState()
        }
        doOnDestroy {

        }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}