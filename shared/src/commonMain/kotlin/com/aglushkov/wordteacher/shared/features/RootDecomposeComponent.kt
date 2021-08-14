package com.aglushkov.wordteacher.shared.features

import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.Router
import com.arkivanov.decompose.RouterState
import com.arkivanov.decompose.pop
import com.arkivanov.decompose.push
import com.arkivanov.decompose.router
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.decompose.value.Value
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

interface RootDecomposeComponent {
    val routerState: Value<RouterState<*, Child>>

    fun openDefinitions()
    fun openArticles()
    fun back()

    class Child(
        val inner: DefinitionsDecomposeComponent
    )

    sealed class ChildConfiguration: Parcelable {
        @Parcelize data class DefinitionConfiguration(val word: String? = null) : RootDecomposeComponent.ChildConfiguration()
        @Parcelize object ArticlesConfiguration : RootDecomposeComponent.ChildConfiguration()
    }
}

class RootDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: RootDecomposeComponent.ChildConfiguration) -> DefinitionsDecomposeComponent
) : RootDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<RootDecomposeComponent.ChildConfiguration, RootDecomposeComponent.Child> =
        router(
            initialConfiguration = RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration(),
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    override val routerState: Value<RouterState<*, RootDecomposeComponent.Child>> = router.state

    private fun resolveChild(
        configuration: RootDecomposeComponent.ChildConfiguration,
        componentContext: ComponentContext
    ): RootDecomposeComponent.Child = RootDecomposeComponent.Child(
            inner = childComponentFactory(componentContext, configuration)
        )

    override fun openDefinitions() {
        router.navigate {
            listOf(it.first())
        }
    }

    override fun openArticles() {
        if (router.state.value.backStack.lastOrNull()?.configuration is RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration) {
            return
        }

        router.navigate {
            listOf(RootDecomposeComponent.ChildConfiguration.ArticlesConfiguration)
        }
    }

    override fun back() {
        if (router.state.value.backStack.size > 1) {
            router.pop()
        }
    }
}
