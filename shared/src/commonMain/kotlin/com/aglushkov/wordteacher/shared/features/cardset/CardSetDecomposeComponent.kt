package com.aglushkov.wordteacher.shared.features.cardset

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVMImpl
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardEnricher
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class CardSetDecomposeComponent (
    initialState: CardSetVM.State,
    cardSetsRepository: CardSetsRepository,
    cardSetRepository: CardSetRepository,
    wordFrequencyGradationProvider: WordFrequencyGradationProvider,
    databaseCardWorker: DatabaseCardWorker,
    componentContext: ComponentContext,
    timeSource: TimeSource,
    idGenerator: IdGenerator,
    analytics: Analytics,
    audioService: AudioService,
    cardEnricher: CardEnricher,
    availableFeatures: CardSetVM.Features
) : CardSetVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetVM.State.serializer()
    ) ?: initialState,
    cardSetsRepository,
    cardSetRepository,
    wordFrequencyGradationProvider,
    databaseCardWorker,
    timeSource,
    idGenerator,
    analytics,
    audioService,
    cardEnricher,
    availableFeatures
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_CardSet"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetVM.State.serializer()
        ) { this.state.value }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
