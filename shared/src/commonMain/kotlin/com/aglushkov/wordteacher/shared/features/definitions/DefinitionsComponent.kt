package com.aglushkov.wordteacher.shared.features.definitions

import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext

open class DefinitionsComponent (
    componentContext: ComponentContext,
    connectivityManager: ConnectivityManager,
    wordDefinitionRepository: WordDefinitionRepository,
    idGenerator: IdGenerator,
    state: DefinitionsVM.State
) : DefinitionsVMImpl(connectivityManager, wordDefinitionRepository, idGenerator, state),
    ComponentContext by componentContext {
}