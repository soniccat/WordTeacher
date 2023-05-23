package com.aglushkov.wordteacher.android_app

import com.aglushkov.wordteacher.android_app.features.add_article.di.DaggerAddArticleComposeComponent
import com.aglushkov.wordteacher.android_app.features.article.di.DaggerArticleComposeComponent
import com.aglushkov.wordteacher.android_app.features.definitions.di.DaggerTabComposeComponent
import com.aglushkov.wordteacher.android_app.features.learning.di.DaggerLearningComponent
import com.aglushkov.wordteacher.android_app.features.learning_session_result.di.DaggerLearningSessionResultComponent
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponentImpl
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.cardset.di.DaggerCardSetComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardsets.di.DaggerCardSetsComponent
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultVM
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
                is MainDecomposeComponent.ChildConfiguration.CardSetsConfiguration ->
                    DaggerCardSetsComponent.builder()
                        .setComponentContext(context)
                        .setState(CardSetsVM.State())
                        .setDeps(appComponent)
                        .build()
                        .cardSetsDecomposeComponent()
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
                        .addArticleDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.LearningConfiguration ->
                    DaggerLearningComponent.builder()
                        .setState(LearningVM.State(configuration.ids, teacherState = null))
                        .setComponentContext(context)
                        .setDeps(appComponent)
                        .build()
                        .learningDecomposeComponent()
                is MainDecomposeComponent.ChildConfiguration.LearningSessionResultConfiguration ->
                    DaggerLearningSessionResultComponent.builder()
                        .setState(LearningSessionResultVM.State(configuration.results))
                        .setComponentContext(context)
                        .setDeps(appComponent)
                        .build()
                        .learningSessionResultDecomposeComponent()
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

