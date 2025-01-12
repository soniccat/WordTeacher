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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.shared.features.SnackbarEventHolderItem
import com.aglushkov.wordteacher.shared.features.SnackbarEventHolder
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.launch
import dev.icerock.moko.resources.compose.localized

private var snackBarUICount by mutableStateOf(0)

val LocalSnackbarUUID = staticCompositionLocalOf {
    uuid4().toString()
}

@Composable
fun BindSnackbarEventHolder(
    eventsHolder: SnackbarEventHolder,
    content: @Composable () -> Unit
) {
    val snackbarUUID = uuid4().toString()
    CompositionLocalProvider(
        LocalSnackbarUUID provides snackbarUUID
    ) {
        DisposableEffect(key1 = "disposable") {
            SnackbarEventHolder.addSource(snackbarUUID, SnackbarEventHolderItem(
                flow = eventsHolder.events,
                handler = { event, withAction ->
                    eventsHolder.onEventHandled(event, withAction)
                }
            ))

            onDispose {
                SnackbarEventHolder.removeSource(snackbarUUID)
            }
        }

        content()
    }
}

@Composable
fun BoxScope.SnackbarUI(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    DisposableEffect("dispose") {
        snackBarUICount += 1
        onDispose {
            snackBarUICount -= 1
        }
    }

    val currentSnackbarUICount = remember { snackBarUICount + 1 }
    val coroutineScope = rememberCoroutineScope()
    val eventHolderItem = SnackbarEventHolder.map[LocalSnackbarUUID.current] ?: return
    val events = eventHolderItem.flow.collectAsState()
    val eventToShow by remember(events) {
        derivedStateOf { events.value.firstOrNull() }
    }

    if (currentSnackbarUICount == snackBarUICount) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Snackbar(snackbarData = it,)
        }

        val snackBarMessage = eventToShow?.text?.localized().orEmpty()
        val snackBarActionText = eventToShow?.actionText?.localized().orEmpty()
        LaunchedEffect(eventToShow) {
            eventToShow?.let { event ->
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        snackBarMessage,
                        snackBarActionText
                    )
                    eventHolderItem.handler(event, result == SnackbarResult.ActionPerformed)
                }
            }
        }
    }
}
