package com.aglushkov.wordteacher.shared.features.definitions.views

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun <T> BottomSheet (
    swipeableState: SwipeableState<T>,
    anchors: Map<Float, T>,
    sheetContent: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    val nestedScrollConnection = rememberNestedScrollConnection(swipeableState, anchors)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(0, swipeableState.offset.value.toInt())
                    }
                }
                .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
                .pointerInput("touchUpDetector") {
                    this.awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Release) {
                                // TODO: handle multiple touches properly
                                val targetValue = swipeableState.targetValue
                                coroutineScope.launch {
                                    swipeableState.animateTo(targetValue)
                                }
                            }
                        }
                    }
                }
                .swipeable(
                    swipeableState,
                    anchors,
                    thresholds = { _, _ -> FixedThreshold(16.dp) },
                    orientation = Orientation.Vertical
                )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                elevation = BottomSheetScaffoldDefaults.SheetElevation,
            ) {
                sheetContent()
            }
        }
    }
}

enum class BottomSheetStates(name: String) {
    Collapsed("Collapsed"),
    Expanded("Expanded"),
    Full("Full"),
}

@ExperimentalMaterialApi
@Composable
private fun <T> rememberNestedScrollConnection(
    swipeableState: SwipeableState<T>,
    anchors: Map<Float, T>,
) = remember(anchors) {
    val min = anchors.keys.minOrNull()!!
    val max = anchors.keys.maxOrNull()!!

    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            return if (delta < 0 && source == NestedScrollSource.Drag) {
                val consumed = swipeableState.performDrag(delta)
                Offset(0f, consumed)
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (source == NestedScrollSource.Drag) {
                val consumed = swipeableState.performDrag(available.y)
                return Offset(x = 0f, y = consumed)
            }

            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = Offset(available.x, available.y).y
            return if (swipeableState.offset.value > min && swipeableState.offset.value < max) {
                swipeableState.performFling(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            swipeableState.performFling(velocity = Offset(available.x, available.y).y)
            return available
        }
    }
}
