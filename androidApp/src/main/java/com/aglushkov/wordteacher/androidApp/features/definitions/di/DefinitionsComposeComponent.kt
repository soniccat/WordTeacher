package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsComposeModule::class])
public interface DefinitionsComposeComponent {
    fun definitionsDecomposeComponent(): DefinitionsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: DefinitionConfiguration): Builder

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComposeComponent
    }

    data class DefinitionConfiguration(val word: String? = null)
}