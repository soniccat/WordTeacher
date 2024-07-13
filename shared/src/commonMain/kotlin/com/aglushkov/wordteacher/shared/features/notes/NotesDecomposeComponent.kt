package com.aglushkov.wordteacher.shared.features.notes

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.consume

class NotesDecomposeComponent (
    componentContext: ComponentContext,
    notesRepository: NotesRepository,
    timeSource: TimeSource,
    state: NotesVM.State,
    analytics: Analytics,
) : NotesVMImpl(
    notesRepository,
    timeSource,
    state,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Notes"

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: state)
    }

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = NotesVM.State.serializer()
        ) {
            state
        }

        restore(instanceState.state)
    }

    private class Handler(val state: NotesVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}