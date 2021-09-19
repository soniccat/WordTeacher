package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
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

interface TabDecomposeComponent {
    val routerState: Value<RouterState<*, Child>>

    fun openDefinitions()
    fun openArticles()
    fun back()

    sealed class Child {
        data class Definitions(val inner: DefinitionsDecomposeComponent): Child()
        data class Articles(val inner: ArticlesDecomposeComponent): Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : ChildConfiguration()
        @Parcelize object ArticlesConfiguration : ChildConfiguration()
    }
}

class TabDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> Any
) : TabDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<TabDecomposeComponent.ChildConfiguration, TabDecomposeComponent.Child> =
        router(
            initialConfiguration = TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration(),
            key = "TabRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    override val routerState: Value<RouterState<*, TabDecomposeComponent.Child>> = router.state

    private fun resolveChild(
        configuration: TabDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): TabDecomposeComponent.Child = when (configuration) {
        is TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> TabDecomposeComponent.Child.Definitions(
            inner = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> TabDecomposeComponent.Child.Articles(
            inner = childComponentFactory(componentContext, configuration) as ArticlesDecomposeComponent
        )
    }

    override fun openDefinitions() {
        router.navigate {
            listOf(it.first())
        }
    }

    override fun openArticles() {
        if (router.state.value.activeChild.configuration is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration) {
            return
        }

        router.navigate {
            it + listOf(TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration)
        }
    }

    override fun back() {
        if (router.state.value.backStack.isNotEmpty()) {
            router.pop()
        }
    }


}
