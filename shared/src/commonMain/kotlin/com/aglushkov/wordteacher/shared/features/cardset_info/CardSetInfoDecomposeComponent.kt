package com.aglushkov.wordteacher.shared.features.cardset_info

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVMImpl
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class CardSetInfoDecomposeComponent(
    componentContext: ComponentContext,
    initialState: CardSetInfoVM.State,
    databaseCardWorker: DatabaseCardWorker,
    cardSetRepository: CardSetRepository,
    analytics: Analytics,
) : CardSetInfoVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = CardSetInfoVM.State.serializer()
    ) ?: initialState,
    databaseCardWorker,
    cardSetRepository,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_CardSetInfo"

    init {
        baseInit(analytics)

        stateKeeper.register(
            key = KEY_STATE,
            strategy = CardSetInfoVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "SAVED_STATE"
    }
}
