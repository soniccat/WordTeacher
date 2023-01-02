package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.articles.ArticlesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.cardsets.CardSetsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.SettingsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.RouterStateChangeHandler
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationOrPopIfExists
import com.aglushkov.wordteacher.shared.general.toClearables
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.router
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map

interface TabDecomposeComponent: Clearable {
    val routerState: Value<RouterState<*, Child>>

    fun openDefinitions()
    fun openCardSets()
    fun openArticles()
    fun openSettings()
    fun openNotes()
    fun back()

    sealed class Child(
        val inner: Clearable
    ): Clearable {
        data class Definitions(val vm: DefinitionsDecomposeComponent): Child(vm)
        data class CardSets(val vm: CardSetsDecomposeComponent): Child(vm)
        data class Articles(val vm: ArticlesDecomposeComponent): Child(vm)
        data class Settings(val vm: SettingsDecomposeComponent): Child(vm)
        data class Notes(val vm: NotesDecomposeComponent): Child(vm)

        override fun onCleared() {
            inner.onCleared()
        }
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : ChildConfiguration()
        @Parcelize object CardSetsConfiguration : ChildConfiguration()
        @Parcelize object ArticlesConfiguration : ChildConfiguration()
        @Parcelize object SettingsConfiguration : ChildConfiguration()
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

    private val routerStateChangeHandler = RouterStateChangeHandler()
    override val routerState: Value<RouterState<*, TabDecomposeComponent.Child>> = router.state.map {
        routerStateChangeHandler.onClearableChanged(it.toClearables())
        it
    }

    private fun resolveChild(
        configuration: TabDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): TabDecomposeComponent.Child = when (configuration) {
        // TODO: refactor
        is TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> TabDecomposeComponent.Child.Definitions(
            vm = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration -> TabDecomposeComponent.Child.CardSets(
            vm = childComponentFactory(componentContext, configuration) as CardSetsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration -> TabDecomposeComponent.Child.Articles(
            vm = childComponentFactory(componentContext, configuration) as ArticlesDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.SettingsConfiguration -> TabDecomposeComponent.Child.Settings(
            vm = childComponentFactory(componentContext, configuration) as SettingsDecomposeComponent
        )
        is TabDecomposeComponent.ChildConfiguration.NotesConfiguration -> TabDecomposeComponent.Child.Notes(
            vm = childComponentFactory(componentContext, configuration) as NotesDecomposeComponent
        )
    }

    override fun openDefinitions() = router.popToRoot()

    override fun openCardSets() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration)

    override fun openArticles() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration)

    override fun openSettings() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.SettingsConfiguration)

    override fun openNotes() =
        router.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.NotesConfiguration)

    override fun back() = router.popIfNotEmpty()

    override fun onCleared() {
        router.navigate({ emptyList() }, { _, _ -> })
    }
}
