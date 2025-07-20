package com.aglushkov.wordteacher.shared.features.learning

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVMImpl
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import com.russhwolf.settings.coroutines.FlowSettings

class LearningDecomposeComponent (
    initialState: LearningVM.State,
    componentContext: ComponentContext,
    databaseCardWorker: DatabaseCardWorker,
    timeSource: TimeSource,
    idGenerator: IdGenerator,
    analytics: Analytics,
    audioService: AudioService,
    settings: SettingStore,
) : LearningVMImpl(
    componentContext.stateKeeper.consume(
        key = KEY_STATE,
        strategy = LearningVM.State.serializer()
    ) ?: initialState,
    databaseCardWorker,
    timeSource,
    idGenerator,
    analytics,
    audioService,
    settings,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Learning"

    init {
        baseInit(analytics)

        stateKeeper.register(
            KEY_STATE,
            strategy = LearningVM.State.serializer()
        ) { this.state }
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }
}
