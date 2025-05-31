package com.aglushkov.wordteacher.shared.features.learning_session_result

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.arkivanov.decompose.ComponentContext

class LearningSessionResultDecomposeComponent (
    initialState: LearningSessionResultVM.State,
    componentContext: ComponentContext,
    cardLoader: CardLoader,
    idGenerator: IdGenerator,
    analytics: Analytics,
) : LearningSessionResultVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = LearningSessionResultVM.State.serializer()
    ) ?: initialState,
    cardLoader,
    idGenerator,
    analytics
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_LearningSession"

    init {
        baseInit(analytics)

        stateKeeper.register(
            KEY_STATE,
            strategy = LearningSessionResultVM.State.serializer()
        ) {
            this.state
        }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
