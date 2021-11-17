package com.aglushkov.wordteacher.androidApp.features.textaction.di

import com.aglushkov.wordteacher.androidApp.features.add_article.di.DaggerAddArticleComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.notes.di.DaggerNotesComponent
import com.aglushkov.wordteacher.androidApp.features.notes.di.NotesComponent
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
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
                is TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration ->
                    DaggerAddArticleComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(AddArticleVM.State(text = config.text.toString()))
                        .setDeps(appComponent)
                        .build()
                        .buildAddArticleDecomposeComponent()
                is TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration ->
                    DaggerNotesComponent.builder()
                        .setComponentContext(context)
                        .setState(
                            NotesVM.State(newNoteText = config.text.toString())
                        )
                        .setDeps(appComponent)
                        .build()
                        .notesDecomposeComponent()
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
