package com.aglushkov.wordteacher.androidApp.features.notes.di

import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [NotesDependencies::class], modules = [NotesModule::class])
public interface NotesComponent {
    fun notesDecomposeComponent(): NotesDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: NotesConfiguration): Builder

        fun setDeps(deps: NotesDependencies): Builder
        fun build(): NotesComponent
    }

    object NotesConfiguration
}

interface NotesDependencies {
    fun routerResolver(): RouterResolver
    fun notesRepository(): NotesRepository
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
}