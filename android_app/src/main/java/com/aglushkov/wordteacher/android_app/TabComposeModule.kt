package com.aglushkov.wordteacher.android_app

import com.aglushkov.wordteacher.android_app.features.articles.di.DaggerArticlesComposeComponent
import com.aglushkov.wordteacher.android_app.features.cardsets.di.DaggerCardSetsComponent
import com.aglushkov.wordteacher.android_app.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.android_app.features.definitions.di.DefinitionsComposeComponent
import com.aglushkov.wordteacher.android_app.features.notes.di.DaggerNotesComponent
import com.aglushkov.wordteacher.android_app.features.settings.di.DaggerSettingsComponent
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class TabComposeModule {
    @Provides
    fun tabDecomposeComponentFactory(
        appComponent: AppComponent
    ): (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration ->
                    DaggerDefinitionsComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(
                            DefinitionsComposeComponent.DefinitionConfiguration(
                                word = configuration.word
                            )
                        )
                        .setDeps(appComponent)
                        .build()
                        .definitionsDecomposeComponent()
                is TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration ->
                    DaggerCardSetsComponent.builder()
                        .setComponentContext(context)
                        .setState(CardSetsVM.State())
                        .setDeps(appComponent)
                        .build()
                        .cardSetsDecomposeComponent()
                is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration ->
                    DaggerArticlesComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(configuration)
                        .setDeps(appComponent)
                        .build()
                        .articlesDecomposeComponent()
                is TabDecomposeComponent.ChildConfiguration.SettingsConfiguration ->
                    DaggerSettingsComponent.builder()
                        .setComponentContext(context)
                        .setState(SettingsVM.State())
                        .setIsDebug(BuildConfig.DEBUG)
                        .setDeps(appComponent)
                        .build()
                        .settingsDecomposeComponent()
                is TabDecomposeComponent.ChildConfiguration.NotesConfiguration ->
                    DaggerNotesComponent.builder()
                        .setComponentContext(context)
                        .setState(NotesVM.State())
                        .setDeps(appComponent)
                        .build()
                        .notesDecomposeComponent()
                else ->
                    throw RuntimeException("Unsupported configuration $configuration")
            }
        }

    @JvmSuppressWildcards
    @Provides
    fun tabDecomposeComponent(
        componentContext: ComponentContext,
        tabDecomposeComponentFactory: (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> Any
    ) : TabDecomposeComponent {
        return TabDecomposeComponentImpl(
            componentContext,
            childComponentFactory = tabDecomposeComponentFactory
        )
    }
}

