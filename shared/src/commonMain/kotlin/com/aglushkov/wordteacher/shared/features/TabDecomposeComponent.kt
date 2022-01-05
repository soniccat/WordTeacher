package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationOrPopIfExists
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.router
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.decompose.value.Value

interface TabDecomposeComponent {
    val routerState: Value<RouterState<*, Child>>

    fun openDefinitions()
    fun openCardSets()
    fun openArticles()
    fun openNotes()
    fun back()

    sealed class Child {
        data class Definitions(val inner: DefinitionsDecomposeComponent): Child()
        data class CardSets(val inner: CardSetsDecomposeComponent): Child()
        data class Articles(val inner: ArticlesDecomposeComponent): Child()
        data class Notes(val inner: NotesDecomposeComponent): Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : ChildConfiguration()
        @Parcelize object CardSetsConfiguration : ChildConfiguration()
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
        is TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration -> TabDecomposeComponent.Child.CardSets(
            inner = childComponentFactory(componentContext, configuration) as CardSetsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> TabDecomposeComponent.Child.Articles(
            inner = childComponentFactory(componentContext, configuration) as ArticlesDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.NotesConfiguration -> TabDecomposeComponent.Child.Notes(
            inner = childComponentFactory(componentContext, configuration) as NotesDecomposeComponent
        )
    }

    override fun openDefinitions() = router.popToRoot()

    override fun openCardSets() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration)

    override fun openArticles() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration)

    override fun openNotes() {
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.NotesConfiguration)
    }

    override fun back() = router.popIfNotEmpty()

}
