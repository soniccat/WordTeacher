package com.aglushkov.wordteacher.android_app.features.notes.di

import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
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
        notesRepository: NotesRepository,
        timeSource: TimeSource,
        state: NotesVM.State
    ) = NotesDecomposeComponent(
        componentContext,
        notesRepository,
        timeSource,
        state
    )
}
