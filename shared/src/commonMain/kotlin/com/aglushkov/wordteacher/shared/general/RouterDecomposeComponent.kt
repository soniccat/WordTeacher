package com.aglushkov.wordteacher.shared.general

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.value.Value

interface RouterDecomposeComponent<TChildConfiguration: Any, TChild: Any> {

    val router: Router<TChildConfiguration, TChild>
    val routerState: Value<RouterState<*, TChild>>
        get() = router.state
}

inline fun <reified C: Any, reified C2: C>
        StackNavigation<C>.pushChildConfigurationIfNotAtTop(
            activeChild: Child.Created<C, *>,
            configuration: C2
        ) {
    if (activeChild is C2) {
        return
    }

    navigate(
        { it + listOf(configuration) },
        {_, _ -> }
    )
}

inline fun <reified C: Any, reified C2: C>
        Router<C,*>.pushChildConfigurationOrPopIfExists(configuration: C2) {
    if (state.value.activeChild.configuration is C2) {
        return
    }

    val i = state.value.backStack.indexOfFirst { it.configuration is C2 }
    if (i >= 0) {
        navigate(
            { it.take(i + 1) },
            { _,_ -> }
        )
    } else {
        navigate(
            { it + listOf(configuration) },
            { _, _ -> }
        )
    }
}

fun Router<*, *>.popIfNotEmpty() {
    if (state.value.backStack.isNotEmpty()) {
        pop()
    }
}

fun <T: Any> Router<T, *>.popToRoot() {
    navigate(
        { listOf(it.first()) },
        { _, _ -> }
    )
}

class RouterStateChangeHandler {
    private var prevClearables: List<Clearable>? = null

    fun onClearableChanged(newClearables: List<Clearable>) {
        prevClearables?.let { prev ->
            handleDiff(prev, newClearables)
        }
        prevClearables = newClearables
    }

    private fun handleDiff(prevClearables: List<Clearable>, newClearables: List<Clearable>) {
        val popedClearables = prevClearables.filter {
            !newClearables.contains(it)
        }
        popedClearables.onEach {
            it.onCleared()
        }
    }
}

fun RouterState<*, Clearable>.toClearables(): List<Clearable> =
    listOfNotNull(
        *backStack.mapNotNull {
            if (it is Child.Created<*, Clearable>) {
                it.instance
            } else {
                null
            }
        }.toTypedArray(),
        activeChild.instance,
    )