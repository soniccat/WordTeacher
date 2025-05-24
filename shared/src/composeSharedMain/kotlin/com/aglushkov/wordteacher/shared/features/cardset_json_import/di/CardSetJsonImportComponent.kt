package com.aglushkov.wordteacher.shared.features.cardset_json_import.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.CardSetJsonImportDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [CardSetJsonImportDependencies::class], modules = [CardSetJsonImportModule::class])
interface CardSetJsonImportComponent {
    fun cardSetsDecomposeComponent(): CardSetJsonImportDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setState(configuration: MainDecomposeComponent.ChildConfiguration.CardSetJsonImportConfiguration): Builder

        fun setDeps(deps: CardSetJsonImportDependencies): Builder
        fun build(): CardSetJsonImportComponent
    }
}

interface CardSetJsonImportDependencies {
    fun cardSetsRepository(): CardSetsRepository
    fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    fun analytics(): Analytics
    fun wordTeacherDictService(): WordTeacherDictService
}