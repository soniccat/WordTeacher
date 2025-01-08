package com.aglushkov.wordteacher.shared.features.textaction

import com.aglushkov.wordteacher.shared.features.SnackbarEventsHolder
import com.aglushkov.wordteacher.shared.features.SnackbarEventsHolderImpl
import com.aglushkov.wordteacher.shared.features.SnackbarEventsHolderRouter
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.RouterDecomposeComponent
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationOrPopIfExists
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

interface TextActionDecomposeComponentRouter {
    fun openArticle(id: Long)
}

interface TextActionDecomposeComponent
    : RouterDecomposeComponent<TextActionDecomposeComponent.ChildConfiguration, TextActionDecomposeComponent.Child>,
    SnackbarEventsHolder {

    var router: TextActionDecomposeComponentRouter?
    val text: CharSequence
    val childStack: Value<ChildStack<ChildConfiguration, Child>>

    fun openDefinitions()
    fun openAddArticle(url: String? = null)
    fun openAddNote()
    fun back()

    fun openCardSets()
    fun openCardSet(state: CardSetVM.State)

    sealed class Child(
        val inner: Clearable
    ): Clearable {
        data class Definitions(val vm: DefinitionsVM): Child(vm)
        data class AddArticle(val vm: AddArticleVM): Child(vm)
        data class AddNote(val vm: NotesVM): Child(vm)

        override fun onCleared() {
            inner.onCleared()
        }
    }

    @Serializable
    sealed class ChildConfiguration {
        @Serializable object DefinitionConfiguration : ChildConfiguration()
        @Serializable data class AddArticleConfiguration(val url: String? = null) : ChildConfiguration()
        @Serializable object AddNoteConfiguration : ChildConfiguration()
    }
}

class TextActionDecomposeComponentImpl(
    override val text: CharSequence,
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: TextActionDecomposeComponent.ChildConfiguration) -> Any
) : TextActionDecomposeComponent,
    ComponentContext by componentContext,
    SnackbarEventsHolder by SnackbarEventsHolderImpl() {

    override var router: TextActionDecomposeComponentRouter? = null
    private val navigation = StackNavigation<TextActionDecomposeComponent.ChildConfiguration>()

    override val childStack: Value<ChildStack<TextActionDecomposeComponent.ChildConfiguration, TextActionDecomposeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = TextActionDecomposeComponent.ChildConfiguration.serializer(), // Or null to disable navigation state saving
            initialConfiguration = TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration,
            handleBackButton = true, // Pop the back stack on back button press
            childFactory = ::resolveChild,
        )

    init {
        snackbarEventRouter = object : SnackbarEventsHolderRouter {
            override fun openArticle(id: Long) {
                router?.openArticle(id)
            }

            override fun openLocalCardSet(cardSetId: Long) {
                openCardSet(CardSetVM.State.LocalCardSet(id = cardSetId))
            }
        }
    }

    private fun resolveChild(
        configuration: TextActionDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): TextActionDecomposeComponent.Child = when (configuration) {
        is TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> TextActionDecomposeComponent.Child.Definitions(
            vm = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration -> TextActionDecomposeComponent.Child.AddArticle(
            vm = childComponentFactory(componentContext, configuration) as AddArticleDecomposeComponent
        )
        is TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration -> TextActionDecomposeComponent.Child.AddNote(
            vm = childComponentFactory(componentContext, configuration) as NotesDecomposeComponent
        )
    }

    override fun openDefinitions() = navigation.popToRoot()

    override fun openAddArticle(url: String?) =
        navigation.pushChildConfigurationOrPopIfExists(
            TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration(url = url)
        )

    override fun openAddNote() =
        navigation.pushChildConfigurationOrPopIfExists(
            TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration
        )

    override fun openCardSets() {
        // TODO: support opening set list
//        router.pushChildConfigurationIfNotAtTop(
//            TextActionDecomposeComponent.ChildConfiguration.CardSetsConfiguration
//        )
    }

    override fun openCardSet(state: CardSetVM.State) {
        // TODO: support opening cardset
    }

    override fun back() = navigation.popIfNotEmpty()
}
