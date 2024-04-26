package com.aglushkov.wordteacher.shared.features.learning_session_result

import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class LearningSessionResultDecomposeComponent (
    state: LearningSessionResultVM.State,
    componentContext: ComponentContext,
    cardLoader: CardLoader,
    idGenerator: IdGenerator
) : LearningSessionResultVMImpl(
    state,
    cardLoader,
    idGenerator
), ComponentContext by componentContext {

//    private val instanceState = instanceKeeper.getOrCreate(::Handler)
    private var instanceState: LearningSessionResultVM.State = stateKeeper.consume(key = KEY_STATE, strategy = LearningSessionResultVM.State.serializer()) ?: state

    init {
        stateKeeper.register(KEY_STATE, strategy = LearningSessionResultVM.State.serializer()) {
            state
        }

        lifecycle.doOnDestroy {
            onCleared()
        }

        restore(instanceState)
    }

//    private class Handler(val state: LearningSessionResultVM.State) : InstanceKeeper.Instance {
//        override fun onDestroy() {}
//    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
