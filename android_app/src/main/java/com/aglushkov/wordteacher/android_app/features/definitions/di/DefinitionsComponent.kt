package com.aglushkov.wordteacher.android_app.features.definitions.di

import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsModule::class])
public interface DefinitionsComposeComponent {
    fun definitionsDecomposeComponent(): DefinitionsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: DefinitionConfiguration): Builder

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComposeComponent
    }

    data class DefinitionConfiguration(val word: String? = null) // TODO: replace with DefinitionsVM.State
}

interface DefinitionsDependencies {
    fun routerResolver(): RouterResolver
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
    fun cardSetsRepository(): CardSetsRepository
    fun dictRepository(): DictRepository
}