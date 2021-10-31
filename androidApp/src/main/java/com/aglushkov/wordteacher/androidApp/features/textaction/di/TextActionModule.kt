package com.aglushkov.wordteacher.androidApp.features.textaction.di

import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponentImpl
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides
import java.lang.RuntimeException

@Module
class TextActionModule {
    @Provides
    fun childComponentFactory(
        appComponent: AppComponent,
        config: TextActionComponent.Config,
    ): (context: ComponentContext, configuration: TextActionDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: TextActionDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration ->
                    DaggerDefinitionsComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(
                            DefinitionsComposeComponent.DefinitionConfiguration(
                                word = config.text.toString()
                            )
                        )
                        .setDeps(appComponent)
                        .build()
                        .definitionsDecomposeComponent()
                else ->
                    throw RuntimeException("Configuration isn't supported: ${configuration}")
            }
        }

    @JvmSuppressWildcards
    @Provides
    fun textActionDecomposeComponent(
        config: TextActionComponent.Config,
        componentContext: ComponentContext,
        childComponentFactory: (context: ComponentContext, configuration: TextActionDecomposeComponent.ChildConfiguration) -> Any
    ): TextActionDecomposeComponent = TextActionDecomposeComponentImpl(
        config.text,
        componentContext,
        childComponentFactory
    )
}
