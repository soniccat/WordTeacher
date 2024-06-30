package com.aglushkov.wordteacher.shared.features.learning

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class LearningDecomposeComponent (
    state: LearningVM.State,
    componentContext: ComponentContext,
    cardLoader: CardLoader,
    database: AppDatabase,
    databaseCardWorker: DatabaseCardWorker,
    timeSource: TimeSource,
    idGenerator: IdGenerator,
    analytics: Analytics,
) : LearningVMImpl(
    state,
    cardLoader,
    databaseCardWorker,
    timeSource,
    idGenerator
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Learning"

    private var instanceState: LearningVM.State = stateKeeper.consume(key = KEY_STATE, strategy = LearningVM.State.serializer()) ?: state

    init {
        baseInit(analytics)

        stateKeeper.register(KEY_STATE, strategy = LearningVM.State.serializer()) {
            state
        }

        restore(instanceState)
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
