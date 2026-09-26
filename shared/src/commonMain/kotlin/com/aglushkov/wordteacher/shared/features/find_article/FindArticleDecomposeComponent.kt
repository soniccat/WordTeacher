package com.aglushkov.wordteacher.shared.features.find_article

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVMImpl
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent.Companion
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.DashboardRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadHeadlineRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy

class FindArticleDecomposeComponent(
    initialState: FindArticleVM.State,
    dashboardRepository: DashboardRepository,
    readHeadlineRepository: ReadHeadlineRepository,
    idGenerator: IdGenerator,
    analytics: Analytics,
    webLinkOpener: WebLinkOpener,
    componentContext: ComponentContext,
): FindArticleVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = FindArticleVM.State.serializer()
    ) ?: initialState,
    dashboardRepository,
    readHeadlineRepository,
    idGenerator,
    analytics,
    webLinkOpener,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_FindArticle"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = FindArticleVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}