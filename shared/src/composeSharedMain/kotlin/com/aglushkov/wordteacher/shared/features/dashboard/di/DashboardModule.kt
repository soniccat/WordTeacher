package com.aglushkov.wordteacher.shared.features.dashboard.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.dashboard.DashboardDecomposeComponent
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class DashboardModule {

    @Provides
    fun dashboardDecomposeComponent(
        componentContext: ComponentContext,
        initialState: DashboardVM.State,
        spaceDashboardService: SpaceDashboardService,
        cardSetsRepository: CardSetsRepository,
        articlesRepository: ArticlesRepository,
        webLinkOpener: WebLinkOpener,
        idGenerator: IdGenerator,
        timeSource: TimeSource,
        analytics: Analytics,
    ) = DashboardDecomposeComponent(
        componentContext,
        initialState,
        spaceDashboardService,
        cardSetsRepository,
        articlesRepository,
        webLinkOpener,
        idGenerator,
        timeSource,
        analytics,
    )
}
