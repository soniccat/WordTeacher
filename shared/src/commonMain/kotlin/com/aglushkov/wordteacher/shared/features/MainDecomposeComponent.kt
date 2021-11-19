package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.ArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM
import com.aglushkov.wordteacher.shared.features.cardset.CardSetDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationIfNotAtTop
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.Router
import com.arkivanov.decompose.RouterState
import com.arkivanov.decompose.pop
import com.arkivanov.decompose.router
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

interface MainDecomposeComponent {
    val routerState: Value<RouterState<*, Child>>
    val dialogRouterState: Value<RouterState<*, Child>>

    fun openAddArticleDialog()
    fun popDialog()
    fun openArticle(id: Long)
    fun openCardSet(id: Long)
    fun back()

    sealed class Child {
        data class Article(val inner: ArticleVM): Child()
        data class CardSet(val inner: CardSetVM): Child()
        data class Tabs(val inner: TabDecomposeComponent): Child()

        data class AddArticle(val inner: AddArticleDecomposeComponent): Child()
        object EmptyDialog: Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class ArticleConfiguration(val id: Long) : ChildConfiguration()
        @Parcelize data class CardSetConfiguration(val id: Long) : ChildConfiguration()
        @Parcelize object TabsConfiguration : ChildConfiguration()

        @Parcelize object AddArticleConfiguration : ChildConfiguration()
        @Parcelize object EmptyDialogConfiguration : ChildConfiguration()
    }
}

class MainDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: MainDecomposeComponent.ChildConfiguration) -> Any
) : MainDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child> =
        router(
            initialConfiguration = MainDecomposeComponent.ChildConfiguration.TabsConfiguration,
            key = "MainRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    private val dialogRouter: Router<MainDecomposeComponent.ChildConfiguration, MainDecomposeComponent.Child> =
        router(
            initialConfiguration = MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration,
            key = "DialogRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    override val routerState: Value<RouterState<*, MainDecomposeComponent.Child>> = router.state
    override val dialogRouterState: Value<RouterState<*, MainDecomposeComponent.Child>> = dialogRouter.state

    private fun resolveChild(
        configuration: MainDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): MainDecomposeComponent.Child = when (configuration) {
        is MainDecomposeComponent.ChildConfiguration.ArticleConfiguration ->
            MainDecomposeComponent.Child.Article(
                inner = childComponentFactory(componentContext, configuration) as ArticleDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.CardSetConfiguration ->
            MainDecomposeComponent.Child.CardSet(
                inner = childComponentFactory(componentContext, configuration) as CardSetDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.TabsConfiguration ->
            MainDecomposeComponent.Child.Tabs(
                inner = childComponentFactory(componentContext, configuration) as TabDecomposeComponent
            )
        is MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration -> MainDecomposeComponent.Child.AddArticle(
            inner = childComponentFactory(componentContext, configuration) as AddArticleDecomposeComponent
        )
        is MainDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration -> MainDecomposeComponent.Child.EmptyDialog

    }

    override fun openArticle(id: Long) =
        router.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.ArticleConfiguration(id)
        )

    override fun openCardSet(id: Long) {
        router.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.CardSetConfiguration(id)
        )
    }

    override fun back() = router.popIfNotEmpty()

    override fun openAddArticleDialog() =
        dialogRouter.pushChildConfigurationIfNotAtTop(
            MainDecomposeComponent.ChildConfiguration.AddArticleConfiguration
        )

    override fun popDialog() = dialogRouter.popIfNotEmpty()
}
