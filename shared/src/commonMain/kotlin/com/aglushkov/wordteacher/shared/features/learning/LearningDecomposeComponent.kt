package com.aglushkov.wordteacher.shared.features.learning

import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume

class LearningDecomposeComponent (
    state: LearningVM.State,
    componentContext: ComponentContext,
    cardLoader: CardLoader,
    database: AppDatabase,
    databaseWorker: DatabaseWorker,
    timeSource: TimeSource,
    idGenerator: IdGenerator
) : LearningVMImpl(
    state,
    cardLoader,
    database,
    databaseWorker,
    timeSource,
    idGenerator
), ComponentContext by componentContext {

    private val instanceState = instanceKeeper.getOrCreate(KEY_STATE) {
        Handler(stateKeeper.consume(KEY_STATE) ?: state)
    }

    init {
        stateKeeper.register(KEY_STATE) {
            save()
        }

        restore(instanceState.state)
    }

    private class Handler(val state: LearningVM.State) : InstanceKeeper.Instance {
        override fun onDestroy() {}
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
