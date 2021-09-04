package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.EmptyDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.Router
import com.arkivanov.decompose.RouterState
import com.arkivanov.decompose.pop
import com.arkivanov.decompose.router
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.decompose.value.Value

interface RootDecomposeComponent {
    val routerState: Value<RouterState<*, Child>>
    val dialogRouterState: Value<RouterState<*, Child>>

    fun openDefinitions()
    fun openArticles()
    fun openAddArticle()
    fun back()
    fun popDialog()

    sealed class Child {
        data class Definitions(val inner: DefinitionsDecomposeComponent): Child()
        data class Articles(val inner: ArticlesDecomposeComponent): Child()
        data class AddArticle(val inner: AddArticleDecomposeComponent): Child()
        object EmptyDialog: Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : RootDecomposeComponent.ChildConfiguration()
        @Parcelize object ArticlesConfiguration : RootDecomposeComponent.ChildConfiguration()
        @Parcelize object AddArticleConfiguration : RootDecomposeComponent.ChildConfiguration()
        @Parcelize object EmptyDialogConfiguration : RootDecomposeComponent.ChildConfiguration()
    }
}

class RootDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> Any
) : RootDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<RootDecomposeComponent.ChildConfiguration, RootDecomposeComponent.Child> =
        router(
            initialConfiguration = RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration(),
            key = "RootRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    private val dialogRouter: Router<RootDecomposeComponent.ChildConfiguration, RootDecomposeComponent.Child> =
        router(
            initialConfiguration = RootDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration,
            key = "DialogRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    override val routerState: Value<RouterState<*, RootDecomposeComponent.Child>> = router.state
    override val dialogRouterState: Value<RouterState<*, RootDecomposeComponent.Child>> = dialogRouter.state

    private fun resolveChild(
        configuration: RootDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): RootDecomposeComponent.Child = when (configuration) {
        is RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> RootDecomposeComponent.Child.Definitions(
            inner = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> RootDecomposeComponent.Child.Articles(
            inner = childComponentFactory(componentContext, configuration) as ArticlesDecomposeComponent
        )
        is RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration -> RootDecomposeComponent.Child.AddArticle(
            inner = childComponentFactory(componentContext, configuration) as AddArticleDecomposeComponent
        )
        is RootDecomposeComponent.ChildConfiguration.EmptyDialogConfiguration -> RootDecomposeComponent.Child.EmptyDialog
    }

    override fun openDefinitions() {
        router.navigate {
            listOf(it.first())
        }
    }

    override fun openArticles() {
        if (router.state.value.activeChild.configuration is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration) {
            return
        }

        router.navigate {
            it + listOf(RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration)
        }
    }

    override fun openAddArticle() {
        if (dialogRouter.state.value.activeChild.configuration is RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration) {
            return
        }

        dialogRouter.navigate {
            it + listOf(RootDecomposeComponent.ChildConfiguration.AddArticleConfiguration)
        }
    }

    override fun back() {
        if (router.state.value.backStack.isNotEmpty()) {
            router.pop()
        }
    }

    override fun popDialog() {
        if (dialogRouter.state.value.backStack.isNotEmpty()) {
            dialogRouter.pop()
        }
    }
}
