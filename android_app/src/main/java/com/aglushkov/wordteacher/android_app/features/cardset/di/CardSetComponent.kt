package com.aglushkov.wordteacher.android_app.features.cardset.di

import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [CardSetDependencies::class], modules = [CardSetModule::class])
public interface CardSetComponent {
    fun cardSetDecomposeComponent(): CardSetDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: CardSetVM.State): Builder

        fun setDeps(deps: CardSetDependencies): Builder
        fun build(): CardSetComponent
    }
}

interface CardSetDependencies {
    fun database(): AppDatabase
    fun databaseWorker(): DatabaseWorker
    fun databaseCardSetWorker(): DatabaseCardWorker
    fun routerResolver(): RouterResolver
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}