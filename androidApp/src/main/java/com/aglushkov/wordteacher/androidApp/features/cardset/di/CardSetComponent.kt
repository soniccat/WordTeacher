package com.aglushkov.wordteacher.androidApp.features.cardset.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Binds
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
    fun routerResolver(): RouterResolver
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}
