package com.aglushkov.wordteacher.shared.features.cardset

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume

class CardSetDecomposeComponent (
    initialState: CardSetVM.State,
    cardSetsRepository: CardSetRepository,
    databaseCardWorker: DatabaseCardWorker,
    componentContext: ComponentContext,
    timeSource: TimeSource,
    idGenerator: IdGenerator
) : CardSetVMImpl(
    initialState,
    cardSetsRepository,
    databaseCardWorker,
    timeSource,
    idGenerator
), ComponentContext by componentContext {

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: initialState)
    }

    init {
        stateKeeper.register(KEY_STATE) {
            state
        }

        restore(instanceState.state)
    }

    private class Handler(val state: CardSetVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
