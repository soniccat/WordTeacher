package com.aglushkov.wordteacher.shared.features.cardset_info

import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.subscribe

class CardSetInfoDecomposeComponent(
    componentContext: ComponentContext,
    initialState: CardSetInfoVM.State,
    databaseCardWorker: DatabaseCardWorker,
    cardSetRepository: CardSetRepository,
) : CardSetInfoVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetInfoVM.State.serializer()
    ) ?: initialState,
    databaseCardWorker,
    cardSetRepository,
), ComponentContext by componentContext {

    init {
        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetInfoVM.State.serializer()
        ) { this.state }

        lifecycle.doOnDestroy {
            onCleared()
        }
    }

    private companion object {
        private const val KEY_STATE = "SAVED_STATE"
    }
}
