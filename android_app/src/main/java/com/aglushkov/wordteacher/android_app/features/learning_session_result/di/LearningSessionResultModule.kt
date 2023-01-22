package com.aglushkov.wordteacher.android_app.features.learning_session_result.di

import com.aglushkov.wordteacher.shared.features.learning_session_result.LearningSessionResultDecomposeComponent
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.data_loader.CardLoader
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class LearningSessionResultModule {

    @Provides
    fun learningDecomposeComponent(
        state: LearningSessionResultVM.State,
        componentContext: ComponentContext,
        cardLoader: CardLoader,
        idGenerator: IdGenerator
    ) = LearningSessionResultDecomposeComponent(
        state,
        componentContext,
        cardLoader,
        idGenerator,
    )
}
