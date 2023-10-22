package com.aglushkov.wordteacher.shared.features.textaction

import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.notes.NotesDecomposeComponent
import com.aglushkov.wordteacher.shared.features.notes.vm.NotesVM
import com.aglushkov.wordteacher.shared.general.Clearable
import com.aglushkov.wordteacher.shared.general.RouterDecomposeComponent
import com.aglushkov.wordteacher.shared.general.RouterStateChangeHandler
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationIfNotAtTop
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationOrPopIfExists
import com.aglushkov.wordteacher.shared.general.toClearables
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.router
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.http.Url

interface TextActionDecomposeComponent
    : RouterDecomposeComponent<TextActionDecomposeComponent.ChildConfiguration, TextActionDecomposeComponent.Child> {

    val text: CharSequence
    override val router: Router<ChildConfiguration, Child>

    fun openDefinitions()
    fun openAddArticle(url: String? = null)
    fun openAddNote()
    fun back()

    fun openCardSets()

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

    sealed class ChildConfiguration: Parcelable {
        @Parcelize object DefinitionConfiguration : ChildConfiguration()
        @Parcelize data class AddArticleConfiguration(val url: String? = null) : ChildConfiguration()
        @Parcelize object AddNoteConfiguration : ChildConfiguration()
    }
}

class TextActionDecomposeComponentImpl(
    override val text: CharSequence,
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: TextActionDecomposeComponent.ChildConfiguration) -> Any
) : TextActionDecomposeComponent, ComponentContext by componentContext {

    override val router: Router<TextActionDecomposeComponent.ChildConfiguration, TextActionDecomposeComponent.Child> =
        router(
            initialConfiguration = TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration,
            key = "TextActionRouter",
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    private val routerStateChangeHandler = RouterStateChangeHandler()
    override val routerState: Value<RouterState<*, TextActionDecomposeComponent.Child>>
        get() = super.routerState.map {
            routerStateChangeHandler.onClearableChanged(it.toClearables())
            it
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

    override fun openDefinitions() = router.popToRoot()

    override fun openAddArticle(url: String?) =
        router.pushChildConfigurationOrPopIfExists(
            TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration(url = url)
        )

    override fun openAddNote() =
        router.pushChildConfigurationOrPopIfExists(
            TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration
        )

    override fun openCardSets() {
        // TODO: support opening set list
//        router.pushChildConfigurationIfNotAtTop(
//            TextActionDecomposeComponent.ChildConfiguration.CardSetsConfiguration
//        )
    }

    override fun back() = router.popIfNotEmpty()
}
