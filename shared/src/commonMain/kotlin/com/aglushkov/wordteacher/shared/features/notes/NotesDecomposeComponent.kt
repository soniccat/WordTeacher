package com.aglushkov.wordteacher.shared.features.notes

import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext

class NotesDecomposeComponent (
    componentContext: ComponentContext,
    notesRepository: NotesRepository,
    timeSource: TimeSource
) : NotesVMImpl(
    notesRepository,
    timeSource,
), ComponentContext by componentContext {
}