package com.aglushkov.wordteacher.shared.general

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value

interface RouterDecomposeComponent<TChildConfiguration: Any, TChild: Any> {

//    val router: Router<TChildConfiguration, TChild>
//    val routerState: Value<RouterState<*, TChild>>
//        get() = router.state
}

@OptIn(ExperimentalDecomposeApi::class)
inline fun <reified C: Any, reified C2: C> StackNavigation<C>.pushChildConfigurationIfNotAtTop(
    configuration: C2
) {
    navigate(
        transformer = { stack -> if (stack.last() is C2) stack else stack + configuration },
        onComplete = { _, _ ->  },
    )
}

inline fun <reified C: Any, reified C2: C> StackNavigation<C>.pushChildConfigurationOrPopIfExists(
    configuration: C2
) {
    navigate( { stack ->
        if (stack.last() is C2) {
            stack
        } else {
            val i = stack.indexOfFirst { it is C2 }
            if (i >= 0) {
                stack.take(i + 1)
            } else {
                stack + configuration
            }

        }
    }, {_, _ ->})
}

fun StackNavigation<*>.popIfNotEmpty() = pop()

fun StackNavigation<*>.popToRoot() = popTo(0)

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

fun ChildStack<*, *>.toClearables(): List<Clearable> =
    listOfNotNull(
        *backStack.mapNotNull {
            it.instance as? Clearable
        }.toTypedArray(),
        active.instance as? Clearable,
    )