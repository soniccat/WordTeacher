package com.aglushkov.wordteacher.android_app.general.views.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun DeletableCell(
    stateKey: Any,
    onClick: () -> Unit,
    onDeleted: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var isSwipedAway by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState()
    val needShowIcon by remember {
        derivedStateOf {
            (-dismissState.offset.value.toInt()).coerceAtLeast(0) != 0
        }
    }

    AnimatedVisibility(
        visible = !isSwipedAway,
        enter = expandVertically(),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = 300,
            )
        )
    ) {
        DisposableEffect("Wait until a disappear animation completes") {
            onDispose {
                if (isSwipedAway) {
                    onDeleted()
                }
            }
        }

        val deleteButtonWidth = (DeleteButtonWidth.value * LocalDensity.current.density).toInt()
        DeleteSwipeable(
            state = dismissState,
            contentModifier = Modifier.fillMaxWidth().clickable { onClick() },
            deleteButtonWidth = deleteButtonWidth,
            background = {
                Box(
                    Modifier.fillMaxSize()
                ) {
                    // To support icon sliding from the right edge
                    if (needShowIcon) {
                        Layout(
                            content = {
                                Image(
                                    painter = rememberVectorPainter(image = Icons.Default.Delete),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .fillMaxSize()
                                        .clickable {
                                            isSwipedAway = true
                                        },
                                    alignment = DeleteIconAlignment(deleteButtonWidth),
                                    contentScale = ContentScale.None
                                )
                            }
                        ) { measurables, constraints ->
                            val iconWidth =
                                (-dismissState.offset.value.toInt()).coerceAtLeast(0)
                            val icon = measurables[0].measure(
                                constraints.constrain(
                                    Constraints(
                                        maxWidth = iconWidth,
                                        maxHeight = constraints.maxHeight,
                                    )
                                )
                            )

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                icon.placeRelative(
                                    x = constraints.maxWidth - iconWidth,
                                    y = 0
                                )
                            }
                        }
                    }
                }
            }
        ) {
            content()
        }
    }
}

@Composable
@ExperimentalMaterialApi
fun DeleteSwipeable(
    state: DismissState,
    contentModifier: Modifier,
    deleteButtonWidth: Int,
    background: @Composable RowScope.() -> Unit,
    dismissContent: @Composable RowScope.() -> Unit
) {
    val deleteButtonWidthFloat = deleteButtonWidth.toFloat()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val anchors = mutableMapOf(0f to DismissValue.Default)
    anchors += -deleteButtonWidthFloat to DismissValue.DismissedToStart

    val minFactor = SwipeableDefaults.StandardResistanceFactor
    val maxFactor = SwipeableDefaults.StiffResistanceFactor
    Box(
        Modifier.swipeable(
            state = state,
            anchors = anchors,
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            resistance = ResistanceConfig(
                basis = deleteButtonWidthFloat,
                factorAtMin = minFactor,
                factorAtMax = maxFactor
            )
        )
    ) {
        Row(
            content = background,
            modifier = Modifier.matchParentSize()
        )
        Row(
            content = dismissContent,
            modifier = Modifier
                .offset { IntOffset(kotlin.math.min(deleteButtonWidth, state.offset.value.roundToInt()), 0) }
                .then(contentModifier)
        )
    }
}

private class DeleteIconAlignment(
    private val deleteButtonWidth: Int
) : Alignment {
    private val innerAlignment = Alignment.CenterStart

    override fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection
    ): IntOffset {
        // shift icon right to center horizontally in the result container
        val leftOffset = (deleteButtonWidth - size.width)/2
        return innerAlignment.align(size, space, layoutDirection).plus(
            IntOffset(leftOffset, 0))
    }
}

private val DeleteButtonWidth = 40.dp
