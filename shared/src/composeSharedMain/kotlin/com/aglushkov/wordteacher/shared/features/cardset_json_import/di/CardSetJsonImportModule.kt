package com.aglushkov.wordteacher.shared.features.cardset_json_import.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.CardSetJsonImportDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class CardSetJsonImportModule {

    @Provides
    fun viewModel(
        componentContext: ComponentContext,
        configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration,
        cardSetsRepository: CardSetsRepository,
        timeSource: TimeSource,
        analytics: Analytics,
    ): CardSetJsonImportDecomposeComponent {
        return CardSetJsonImportDecomposeComponent(
            componentContext,
            configuration,
            cardSetsRepository,
            timeSource,
            analytics,
        )
    }
}