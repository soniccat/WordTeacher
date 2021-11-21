package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.*
import com.aglushkov.wordteacher.androidApp.features.articles.views.roundToMax

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun DeletableCell(
    stateKey: Any,
    onClick: () -> Unit,
    onDeleted: () -> Unit,
    threshold: Float = 0.3f,
    content: @Composable RowScope.() -> Unit
) {
    key(stateKey) {
        var isSwipedAway by remember { mutableStateOf(false) }
        val dismissState = rememberDismissState(
            confirmStateChange = {
                if (it == DismissValue.DismissedToStart) {
                    isSwipedAway = true
                }
                true
            }
        )

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
            val dismissThreshold: (DismissDirection) -> ThresholdConfig =
                { FractionalThreshold(threshold) }
            SwipeToDismiss(
                state = dismissState,
                modifier = Modifier.clickable { onClick() },
                directions = setOf(DismissDirection.EndToStart),
                dismissThresholds = dismissThreshold,
                background = {
                    Box(
                        Modifier.fillMaxSize()
                    ) {
                        val density = LocalDensity.current
                        // To support icon sliding from the right edge
                        Layout(
                            content = {
                                Image(
                                    painter = rememberVectorPainter(image = Icons.Default.Delete),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .fillMaxSize(),
                                    alignment = DeleteIconAlignment(deleteButtonWidth),
                                    contentScale = ContentScale.None
                                )
                            }
                        ) { measurables, constraints ->
                            val resultFraction = when (dismissState.progress.to) {
                                DismissValue.DismissedToStart -> dismissState.progress.fraction * (1.0f / threshold)
                                else -> 0f
                            }.roundToMax(1.0f)

                            val iconWidth = (deleteButtonWidth * resultFraction).toInt()
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
            ) {
                content()
            }
        }
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
