package com.aglushkov.wordteacher.shared.features.textaction

import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.general.RouterDecomposeComponent
import com.aglushkov.wordteacher.shared.general.popIfNotEmpty
import com.aglushkov.wordteacher.shared.general.popToRoot
import com.aglushkov.wordteacher.shared.general.pushChildConfigurationIfNotAtTop
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.Router
import com.arkivanov.decompose.router
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

interface TextActionDecomposeComponent
    : RouterDecomposeComponent<TextActionDecomposeComponent.ChildConfiguration, TextActionDecomposeComponent.Child> {

    val text: CharSequence
    override val router: Router<ChildConfiguration, Child>

    fun openDefinitions()
    fun openAddArticle()
    fun openAddNote()
    fun back()

    sealed class Child {
        data class Definitions(val inner: DefinitionsDecomposeComponent): Child()
        data class AddArticle(val inner: Any): Child()
        data class AddNote(val inner: Any): Child()
    }

    sealed class ChildConfiguration: Parcelable {
        @Parcelize object DefinitionConfiguration : ChildConfiguration()
        @Parcelize object AddArticleConfiguration : ChildConfiguration()
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

    private fun resolveChild(
        configuration: TextActionDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): TextActionDecomposeComponent.Child = when (configuration) {
        is TextActionDecomposeComponent.ChildConfiguration.DefinitionConfiguration -> TextActionDecomposeComponent.Child.Definitions(
            inner = childComponentFactory(componentContext, configuration) as DefinitionsDecomposeComponent
        )
        is TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration -> TextActionDecomposeComponent.Child.AddArticle(
            inner = childComponentFactory(componentContext, configuration)
        )
        is TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration -> TextActionDecomposeComponent.Child.AddNote(
            inner = childComponentFactory(componentContext, configuration)
        )
    }

    override fun openDefinitions() = router.popToRoot()

    override fun openAddArticle() =
        router.pushChildConfigurationIfNotAtTop(
            TextActionDecomposeComponent.ChildConfiguration.AddArticleConfiguration
        )

    override fun openAddNote() =
        router.pushChildConfigurationIfNotAtTop(
            TextActionDecomposeComponent.ChildConfiguration.AddNoteConfiguration
        )

    override fun back() = router.popIfNotEmpty()
}
