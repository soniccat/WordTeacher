package com.aglushkov.wordteacher.androidApp.features.notes.di

import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class NotesModule {
    @Provides
    fun notesDecomposeComponent(
        componentContext: ComponentContext,
        configuration: NotesComponent.NotesConfiguration,
        notesRepository: NotesRepository,
        timeSource: TimeSource
    ) = NotesDecomposeComponent(
        componentContext,
        notesRepository,
        timeSource
    )
}
