package com.aglushkov.wordteacher.shared.features.cardset_json_import

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.CardSetJsonImportVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext

class CardSetJsonImportDecomposeComponent (
    componentContext: ComponentContext,
    configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration,
    cardSetsRepository: CardSetsRepository,
    timeSource: TimeSource,
) : CardSetJsonImportVMImpl(
    configuration,
    cardSetsRepository,
    timeSource,
), ComponentContext by componentContext {
}
