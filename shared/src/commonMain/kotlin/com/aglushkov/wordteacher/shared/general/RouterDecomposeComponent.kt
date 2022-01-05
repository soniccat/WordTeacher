package com.aglushkov.wordteacher.shared.general

import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.value.Value

interface RouterDecomposeComponent<TChildConfiguration: Any, TChild: Any> {

    val router: Router<TChildConfiguration, TChild>
    val routerState: Value<RouterState<*, TChild>>
        get() = router.state
}

inline fun <reified C: Any, reified C2: C>
        Router<C,*>.pushChildConfigurationIfNotAtTop(configuration: C2) {
    if (state.value.activeChild.configuration is C2) {
        return
    }

    navigate {
        it + listOf(configuration)
    }
}

inline fun <reified C: Any, reified C2: C>
        Router<C,*>.pushChildConfigurationOrPopIfExists(configuration: C2) {
    if (state.value.activeChild.configuration is C2) {
        return
    }

    val i = state.value.backStack.indexOfFirst { it.configuration is C2 }
    if (i >= 0) {
        navigate {
            it.take(i + 1)
        }
    } else {
        navigate {
            it + listOf(configuration)
        }
    }
}

fun Router<*, *>.popIfNotEmpty() {
    if (state.value.backStack.isNotEmpty()) {
        pop()
    }
}

fun <T: Any> Router<T, *>.popToRoot() {
    navigate {
        listOf(it.first())
    }
}