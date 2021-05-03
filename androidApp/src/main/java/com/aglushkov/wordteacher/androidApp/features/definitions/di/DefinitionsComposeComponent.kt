package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import org.jetbrains.annotations.Nullable

@FragmentComp
@Component(dependencies = [DefinitionsComposeDependencies::class], modules = [DefinitionsComposeModule::class])
public interface DefinitionsComposeComponent {
    fun definitionsComponent(): DefinitionsDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder

        fun setDeps(deps: DefinitionsComposeDependencies): Builder
        fun build(): DefinitionsComposeComponent
    }
}

interface DefinitionsComposeDependencies {
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
}
