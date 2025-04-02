package com.aglushkov.wordteacher.shared.features.dashboard.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.di.ArticleModule
import com.aglushkov.wordteacher.shared.features.cardset_info.CardSetInfoDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.dashboard.DashboardDecomposeComponent
import com.aglushkov.wordteacher.shared.features.dashboard.vm.DashboardVM
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsModule
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.WebLinkOpener
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.service.SpaceDashboardService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.BindsInstance
import dagger.Component

@Component(
    dependencies = [DashboardDependencies::class],
    modules = [DashboardModule::class]
)
interface DashboardComponent {
    fun dashboardDecomposeComponent(): DashboardDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance
        fun setInitialState(state: DashboardVM.State): Builder

        fun setDeps(deps: DashboardDependencies): Builder
        fun build(): DashboardComponent
    }
}

interface DashboardDependencies {
    fun spaceDashboardService(): SpaceDashboardService
    fun cardSetsRepository(): CardSetsRepository
    fun articlesRepository(): ArticlesRepository
    fun webLinkOpener(): WebLinkOpener
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    fun analytics(): Analytics
}
