package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.androidApp.features.add_article.di.DaggerAddArticleComposeComponent
import com.aglushkov.wordteacher.androidApp.features.article.di.DaggerArticleComponent
import com.aglushkov.wordteacher.androidApp.features.article.di.DaggerArticleComposeComponent
import com.aglushkov.wordteacher.androidApp.features.articles.di.DaggerArticlesComposeComponent
import com.aglushkov.wordteacher.androidApp.features.cardset.di.DaggerCardSetComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerDefinitionsComposeComponent
import com.aglushkov.wordteacher.androidApp.features.definitions.di.DaggerTabComposeComponent
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides

@Module
class MainComposeModule {
    @Provides
    fun mainDecomposeComponentFactory(
        appComponent: AppComponent
    ): (context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration) -> Any =
        { context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration ->
            when (configuration) {
                is MainDecomposeComponent.ChildConfiguration.ArticleConfiguration ->
                    DaggerArticleComposeComponent.builder()
                        .setComponentContext(context)
                        .setDeps(appComponent)
                        .setDefinitionsDeps(appComponent)
                        .setConfiguration(configuration)
                        .build()
                        .articleDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.CardSetConfiguration ->
                    DaggerCardSetComponent.builder()
                        .setComponentContext(context)
                        .setDeps(appComponent)
                        .setState(CardSetVM.State(configuration.id))
                        .build()
                        .cardSetDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.TabsConfiguration ->
                    DaggerTabComposeComponent.builder()
                        .setComponentContext(context)
                        .setAppComponent(appComponent)
                        .setWord("owl")
                        .build()
                        .tabDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration ->
                    DaggerAddArticleComposeComponent.builder()
                        .setComponentContext(context)
                        .setConfiguration(AddArticleVM.State())
                        .setDeps(appComponent)
                        .build()
                        .buildAddArticleDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration ->
                    Any()
            }

        }

    @JvmSuppressWildcards
    @Provides
    fun mainDecomposeComponent(
        componentContext: ComponentContext,
        mainDecomposeComponentFactory: (context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration) -> Any
    ) : MainDecomposeComponent {
        return MainDecomposeComponentImpl(
            componentContext,
            childComponentFactory = mainDecomposeComponentFactory
        )
    }
}

