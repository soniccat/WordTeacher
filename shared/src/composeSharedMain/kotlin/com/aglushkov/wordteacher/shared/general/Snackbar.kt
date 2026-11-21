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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.aglushkov.wordteacher.shared.features.SnackbarEventHolder
import com.aglushkov.wordteacher.shared.general.resources.resolve
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

private val LocalSnackbarEventHolder = staticCompositionLocalOf<SnackbarEventHolder?> { null }

// Registry of SnackbarUI instances composed within a single composition
// (a single activity or window). The SnackbarUI laid out deepest in the
// layout hierarchy is the top one: only it calls
// snackbarHostState.showSnackbar, all the SnackbarUIs above it are ignored.
// When it leaves composition the next deepest one takes over and shows any
// not yet handled event.
//
// Unlike the old global snackBarUICount counter the registry is remembered
// inside BindSnackbarEventHolder, so instances of different activities
// never affect each other.
private val LocalSnackbarUIRegistry = staticCompositionLocalOf<SnackbarUIRegistry?> { null }

private class SnackbarUIRegistry {
    private class Registration(
        val id: Any,
        val depth: Int,
    )

    private var registrations by mutableStateOf(listOf<Registration>())

    val topId: Any?
        get() = registrations.asReversed().maxByOrNull { it.depth }?.id

    fun register(id: Any) {
        update(id, 0)
    }

    fun updateDepth(id: Any, depth: Int) {
        update(id, depth)
    }

    fun unregister(id: Any) {
        if (registrations.none { it.id === id }) return
        registrations = registrations.filter { it.id !== id }
    }

    private fun update(id: Any, depth: Int) {
        if (registrations.any { it.id === id && it.depth == depth }) return
        registrations = registrations.filter { it.id !== id } + Registration(id, depth)
    }
}

@Composable
fun BindSnackbarEventHolder(
    eventsHolder: SnackbarEventHolder,
    content: @Composable () -> Unit
) {
    val registry = remember { SnackbarUIRegistry() }
    CompositionLocalProvider(
        LocalSnackbarEventHolder provides eventsHolder,
        LocalSnackbarUIRegistry provides registry,
    ) {
        content()
    }
}

/**
 * Shows snackbars for the events of the SnackbarEventHolder bound by
 * BindSnackbarEventHolder.
 *
 * If several SnackbarUIs are composed at the same time only the top one
 * calls snackbarHostState.showSnackbar, all the instances above it in the
 * layout hierarchy are ignored. The top one is resolved automatically:
 * the deeper a SnackbarUI is placed in the layout hierarchy the more
 * priority it has, so a SnackbarUI placed inside a screen (for example
 * above a bottom bar) takes precedence over a SnackbarUI placed at the
 * root of an activity.
 */
@Composable
fun BoxScope.SnackbarUI(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // The holder is provided by BindSnackbarEventHolder and is available
    // during the first composition, so there is no subscription race
    val eventsHolder = LocalSnackbarEventHolder.current ?: return
    val registry = LocalSnackbarUIRegistry.current ?: return
    val id = remember { Any() }

    DisposableEffect(id) {
        registry.register(id)
        onDispose {
            registry.unregister(id)
        }
    }

    // reports the depth of this SnackbarUI in the layout hierarchy,
    // the deepest SnackbarUI becomes the top one
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .onGloballyPositioned { coordinates ->
                registry.updateDepth(id, coordinates.layoutDepth())
            }
    ) {
        Snackbar(snackbarData = it,)
    }

    // topId is null during the first composition (registrations happen
    // in the effect phase) and triggers a recomposition as soon as
    // this instance is registered
    if (registry.topId !== id) {
        return
    }

    val events = eventsHolder.events.collectAsState()
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
                eventsHolder.onEventHandled(event, result == SnackbarResult.ActionPerformed)
                Logger.v("handled $snackBarMessage", TAG)
            }
        }
    }
}

private fun LayoutCoordinates.layoutDepth(): Int {
    var depth = 0
    var coordinates = parentCoordinates
    while (coordinates != null) {
        depth += 1
        coordinates = coordinates.parentCoordinates
    }
    return depth
}

private const val TAG = "Snackbar"
