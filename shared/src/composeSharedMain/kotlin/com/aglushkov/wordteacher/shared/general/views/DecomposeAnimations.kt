package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimator

internal val defaultChildAnimationSpec: FiniteAnimationSpec<Float> = tween()

@ExperimentalDecomposeApi
fun slideFromBottom(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction, content ->
        if (direction == Direction.ENTER_FRONT || direction == Direction.EXIT_FRONT) {
            content(
                Modifier.offsetYFactor(factor = factor)
            )
        } else {
            content(Modifier)
        }
    }

@ExperimentalDecomposeApi
fun slideFromRight(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction, content ->
        if (direction == Direction.ENTER_FRONT || direction == Direction.EXIT_FRONT) {
            content(
                Modifier.offsetXFactor(factor = factor)
            )
        } else {
            content(Modifier)
        }
    }

private fun Modifier.offsetYFactor(factor: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = 0, y = (placeable.height.toFloat() * factor).toInt())
        }
    }

private fun Modifier.offsetXFactor(factor: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = (placeable.width.toFloat() * factor).toInt(), y = 0)
        }
    }