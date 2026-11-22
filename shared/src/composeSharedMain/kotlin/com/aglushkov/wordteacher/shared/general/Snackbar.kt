package com.aglushkov.wordteacher.shared.general

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.shared.features.SnackbarEventHolder
import com.aglushkov.wordteacher.shared.general.resources.resolve
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

// Snackbar nesting level of the current composition, zero at the root of
// an activity. The composed SnackbarUI
// with the highest level takes precedence over all the SnackbarUIs below.
private val LocalSnackbarLevel = staticCompositionLocalOf { 0 }

// Snackbar state of a single composition (a single activity or window):
// the source of the events to be shown and the registry of the composed
// SnackbarUI instances. Provided by BindSnackbarEventHolder.
private val LocalSnackbarState = staticCompositionLocalOf<SnackbarState?> { null }

private class SnackbarState(
    val eventsHolder: SnackbarEventHolder,
) {
    val registry = SnackbarUIRegistry()
}

// Registry of SnackbarUI instances composed within a single composition
// (a single activity or window). The registered SnackbarUI with the
// highest level is the top one: only it calls
// snackbarHostState.showSnackbar, all the SnackbarUIs below it are ignored.
// When it leaves composition the next one takes over and shows any
// not yet handled event. Among SnackbarUIs with the same level the last
// registered one wins.
private class SnackbarUIRegistry {
    private class Registration(
        val id: Any,
        val level: Int,
    )

    private var registrations by mutableStateOf(listOf<Registration>())

    val topId: Any?
        get() = registrations.asReversed().maxByOrNull { it.level }?.id

    fun register(id: Any, level: Int) {
        if (registrations.any { it.id === id && it.level == level }) return
        registrations = registrations.filter { it.id !== id } + Registration(id, level)
    }

    fun unregister(id: Any) {
        if (registrations.none { it.id === id }) return
        registrations = registrations.filter { it.id !== id }
    }
}

@Composable
fun BindSnackbarEventHolder(
    eventsHolder: SnackbarEventHolder,
    content: @Composable () -> Unit
) {
    val state = remember { SnackbarState(eventsHolder) }
    CompositionLocalProvider(
        LocalSnackbarState provides state,
    ) {
        content()
    }
}

/**
 * Shows snackbars for the events of the SnackbarEventHolder bound by
 * BindSnackbarEventHolder.
 *
 * If several SnackbarUIs are composed at the same time only the top one
 * calls snackbarHostState.showSnackbar, all the instances below it are
 * ignored. The top one is resolved automatically: the composed SnackbarUI
 * with the highest LocalSnackbarLevel wins, so place a SnackbarUI deeper
 * in the screen hierarchy into a container providing a higher level
 * (CustomDialogUI does it for dialogs, TabsUI for the content above
 * the bottom bar).
 */
@Composable
fun BoxScope.SnackbarUI(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) = CompositionLocalProvider(
    LocalSnackbarLevel provides LocalSnackbarLevel.current + 1
) {
    SnackbarUIInternal(snackbarHostState)
}

@Composable
private fun BoxScope.SnackbarUIInternal(
    snackbarHostState: SnackbarHostState,
) {
    // The state is provided by BindSnackbarEventHolder and is available
    // during the first composition, so there is no subscription race
    val state = LocalSnackbarState.current ?: return
    val level = LocalSnackbarLevel.current
    val id = remember { Any() }

    DisposableEffect(id, level) {
        state.registry.register(id, level)
        onDispose {
            state.registry.unregister(id)
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Snackbar(snackbarData = it,)
    }

    // topId is null during the first composition (registrations happen
    // in the effect phase) and triggers a recomposition as soon as
    // this instance is registered
    if (state.registry.topId !== id) {
        return
    }

    val events = state.eventsHolder.events.collectAsState()
    val eventToShow = events.value.firstOrNull()

    val snackBarMessage = eventToShow?.text?.resolve().orEmpty()
    val snackBarActionText = eventToShow?.actionText?.resolve()
    LaunchedEffect(eventToShow) {
        eventToShow?.let { event ->
            // launch in the scope of the effect, so that the showing
            // coroutine is cancelled together with the effect (when this
            // SnackbarUI leaves composition or loses the top priority);
            // the event is kept, so the same snackbar is shown again by
            // the SnackbarUI which is top afterwards, for example after
            // coming back to the previous screen
            launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                Logger.v("exceptionHandler: " + throwable.message.orEmpty(), TAG)
            }) {
                Logger.v("showSnackbar $snackBarMessage", TAG)
                val result = snackbarHostState.showSnackbar(
                    snackBarMessage,
                    snackBarActionText
                )
                state.eventsHolder.onEventHandled(event, result == SnackbarResult.ActionPerformed)
                Logger.v("handled $snackBarMessage", TAG)
            }
        }
    }
}

private const val TAG = "Snackbar"
