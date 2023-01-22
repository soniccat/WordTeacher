package com.aglushkov.wordteacher.android_app.features.cardsets.di

import com.aglushkov.wordteacher.android_app.di.FragmentComp
import com.aglushkov.wordteacher.android_app.general.RouterResolver

import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@FragmentComp
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
    fun routerResolver(): RouterResolver
    fun cardSetsRepository(): CardSetsRepository
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}