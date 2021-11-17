package com.aglushkov.wordteacher.androidApp.features.cardsets.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp

import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [CardSetsDependencies::class], modules = [CardSetsComposeModule::class])
interface CardSetsComposeComponent {
    fun cardSetsDecomposeComponent(): CardSetsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: CardSetsVM.State): Builder

        fun setDeps(deps: CardSetsDependencies): Builder
        fun build(): CardSetsComposeComponent
    }
}
