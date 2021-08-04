package com.aglushkov.wordteacher.shared.features.definitions

import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume

class DefinitionsDecomposeComponent (
    componentContext: ComponentContext,
    word: String?,
    connectivityManager: ConnectivityManager,
    wordDefinitionRepository: WordDefinitionRepository,
    idGenerator: IdGenerator
) : DefinitionsVMImpl(connectivityManager, wordDefinitionRepository, idGenerator, DefinitionsVM.State(word = word)),
    ComponentContext by componentContext {

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: DefinitionsVM.State(word = word))
    }

    init {
        stateKeeper.register(KEY_STATE) {
            state
        }

        restore(instanceState.state)
    }

    private class Handler(val state: DefinitionsVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
