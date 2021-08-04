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

    fun onNextChild()
    fun onPrevChild()

    class Child(
        val inner: DefinitionsDecomposeComponent
    )

}

class RootDecomposeComponentImpl(
    componentContext: ComponentContext,
    val childComponentFactory: (context: ComponentContext, configuration: ChildConfiguration) -> DefinitionsDecomposeComponent
) : RootDecomposeComponent, ComponentContext by componentContext {

    private val router: Router<ChildConfiguration, RootDecomposeComponent.Child> =
        router(
            initialConfiguration = ChildConfiguration(),
            handleBackButton = true,
            childFactory = ::resolveChild
        )

    override val routerState: Value<RouterState<*, RootDecomposeComponent.Child>> = router.state

    private fun resolveChild(configuration: ChildConfiguration, componentContext: ComponentContext): RootDecomposeComponent.Child =
        RootDecomposeComponent.Child(
            inner = childComponentFactory(componentContext, configuration)
        )

    override fun onNextChild() {
        router.push(ChildConfiguration(word = "clear"))
    }

    override fun onPrevChild() {
        router.pop()
    }
}

@Parcelize
data class ChildConfiguration(val word: String? = null, val id: String = uuid4().toString()) : Parcelable
