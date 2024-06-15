package com.aglushkov.wordteacher.shared.features.cardset

import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class CardSetDecomposeComponent (
    initialState: CardSetVM.State,
    cardSetsRepository: CardSetRepository,
    wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    databaseCardWorker: DatabaseCardWorker,
    componentContext: ComponentContext,
    timeSource: TimeSource,
    idGenerator: IdGenerator
) : CardSetVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetVM.State.serializer()
    ) ?: initialState,
    cardSetsRepository,
    wordFrequencyGradationProvider,
    databaseCardWorker,
    timeSource,
    idGenerator
), ComponentContext by componentContext {

    init {
        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetVM.State.serializer()
        ) { this.state }

        lifecycle.doOnDestroy {
            onCleared()
        }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
