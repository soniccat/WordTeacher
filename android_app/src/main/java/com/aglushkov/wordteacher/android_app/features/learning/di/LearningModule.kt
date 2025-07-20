package com.aglushkov.wordteacher.android_app.features.learning.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.learning.LearningDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.AudioService
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class LearningModule {

    @Provides
    fun learningDecomposeComponent(
        state: LearningVM.State,
        componentContext: ComponentContext,
        databaseCardWorker: DatabaseCardWorker,
        timeSource: TimeSource,
        idGenerator: IdGenerator,
        analytics: Analytics,
        audioService: AudioService,
        settings: SettingStore,
    ) = LearningDecomposeComponent(
        state,
        componentContext,
        databaseCardWorker,
        timeSource,
        idGenerator,
        analytics,
        audioService,
        settings,
    )
}
