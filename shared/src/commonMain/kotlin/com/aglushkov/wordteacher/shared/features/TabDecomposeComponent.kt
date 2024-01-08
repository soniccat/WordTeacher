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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.serialization.Serializable

interface TabDecomposeComponent: Clearable {
    val childStack: Value<ChildStack<*, Child>>

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

    @Serializable
    sealed class ChildConfiguration {
        @Serializable data class DefinitionConfiguration(val word: String? = null) : ChildConfiguration()
        @Serializable object CardSetsConfiguration : ChildConfiguration()
        @Serializable object ArticlesConfiguration : ChildConfiguration()
        @Serializable object SettingsConfiguration : ChildConfiguration()
        @Serializable object NotesConfiguration : ChildConfiguration()
    }
}

class TabDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: TabDecomposeComponent.ChildConfiguration) -> Any
) : TabDecomposeComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<TabDecomposeComponent.ChildConfiguration>()

    override val childStack: Value<ChildStack<TabDecomposeComponent.ChildConfiguration, TabDecomposeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = TabDecomposeComponent.ChildConfiguration.serializer(), // Or null to disable navigation state saving
            initialConfiguration = TabDecomposeComponent.ChildConfiguration.DefinitionConfiguration(),
            handleBackButton = true, // Pop the back stack on back button press
            childFactory = ::resolveChild,
        )

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

    override fun openDefinitions() = navigation.popToRoot()

    override fun openCardSets() =
        navigation.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.CardSetsConfiguration)

    override fun openArticles() =
        navigation.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.ArticlesConfiguration)

    override fun openSettings() =
        navigation.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.SettingsConfiguration)

    override fun openNotes() =
        navigation.pushChildConfigurationOrPopIfExists(TabDecomposeComponent.ChildConfiguration.NotesConfiguration)

    override fun back() = navigation.popIfNotEmpty()

    override fun onCleared() {
        navigation.navigate({ emptyList() }, { _, _ -> })
    }
}
