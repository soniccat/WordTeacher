package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationIfNotAtTop
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
    fun openNotes()
    fun back()

    sealed class Child {
        data class Definitions(val inner: DefinitionsDecomposeComponent): Child()
        data class Articles(val inner: ArticlesDecomposeComponent): Child()
        data class Notes(val inner: NotesDecomposeComponent): Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : ChildConfiguration()
        @Parcelize object ArticlesConfiguration : ChildConfiguration()
        @Parcelize object NotesConfiguration : ChildConfiguration()
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
        // TODO: refactor
        is TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> TabDecomposeComponent.Child.Definitions(
            inner = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> TabDecomposeComponent.Child.Articles(
            inner = childComponentFactory(componentContext, configuration) as ArticlesDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.NotesConfiguration -> TabDecomposeComponent.Child.Notes(
            inner = childComponentFactory(componentContext, configuration) as NotesDecomposeComponent
        )
    }

    override fun openDefinitions() = router.popToRoot()

    override fun openArticles() =
        router.pushChildConfigurationIfNotAtTop(TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration)

    override fun openNotes() {
        router.pushChildConfigurationIfNotAtTop(TabDecomposeComponent.ChildConfiguration.NotesConfiguration)
    }

    override fun back() = router.popIfNotEmpty()

}
