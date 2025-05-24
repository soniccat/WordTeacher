package com.aglushkov.wordteacher.shared.features.cardset_json_import

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.CardSetJsonImportVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class CardSetJsonImportDecomposeComponent (
    componentContext: ComponentContext,
    configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration,
    cardSetsRepository: CardSetsRepository,
    timeSource: TimeSource,
    analytics: Analytics,
    wordTeacherDictService: WordTeacherDictService,
) : CardSetJsonImportVMImpl(
    configuration,
    cardSetsRepository,
    timeSource,
    wordTeacherDictService,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_CardSetJsonImport"

    init {
        baseInit(analytics)
    }
}
