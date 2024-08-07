package com.aglushkov.wordteacher.shared.features.cardsets.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.service.SpaceCardSetSearchService
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [CardSetsDependencies::class], modules = [CardSetsModule::class])
interface CardSetsComponent {
    fun cardSetsDecomposeComponent(): CardSetsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: CardSetsVM.State): Builder

        fun setDeps(deps: CardSetsDependencies): Builder
        fun build(): CardSetsComponent
    }
}

interface CardSetsDependencies {
    fun cardSetsRepository(): CardSetsRepository
    fun spaceCardSetService(): SpaceCardSetService
    fun spaceCardSetSearchService(): SpaceCardSetSearchService
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    fun cardSetsFeatures(): CardSetsVM.Features
    fun appDatabase(): AppDatabase
    fun analytics(): Analytics
}