package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.childAnimator

internal val defaultChildAnimationSpec: FiniteAnimationSpec<Float> = tween()

@ExperimentalDecomposeApi
fun slideFromBottom(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): ChildAnimator =
    childAnimator(animationSpec = animationSpec) { factor, direction, content ->
        content(
            Modifier.offsetYFactor(factor = factor)
        )
    }

@ExperimentalDecomposeApi
fun slideFromRight(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): ChildAnimator =
    childAnimator(animationSpec = animationSpec) { factor, direction, content ->
        content(
            Modifier.offsetYFactor(factor = factor)
        )
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