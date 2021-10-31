package com.aglushkov.wordteacher.shared.general

import com.arkivanov.decompose.Router
import com.arkivanov.decompose.RouterState
import com.arkivanov.decompose.pop
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