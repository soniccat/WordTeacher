package com.aglushkov.wordteacher.shared.features.dashboard

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVMImpl
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVMIMpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadCardSetRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadHeadlineRepository
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class DashboardDecomposeComponent(
    componentContext: ComponentContext,
    initialState: DashboardVM.State,
    spaceDashboardService: SpaceDashboardService,
    cardSetsRepository: CardSetsRepository,
    articlesRepository: ArticlesRepository,
    readHeadlineRepository: ReadHeadlineRepository,
    readCardSetRepository: ReadCardSetRepository,
    webLinkOpener: WebLinkOpener,
    idGenerator: IdGenerator,
    timeSource: TimeSource,
    analytics: Analytics,
) : DashboardVMIMpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = DashboardVM.State.serializer()
    ) ?: initialState,
    spaceDashboardService,
    cardSetsRepository,
    articlesRepository,
    readHeadlineRepository,
    readCardSetRepository,
    webLinkOpener,
    idGenerator,
    timeSource,
    analytics,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Dashboard"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = DashboardVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "SAVED_STATE"
    }
}
