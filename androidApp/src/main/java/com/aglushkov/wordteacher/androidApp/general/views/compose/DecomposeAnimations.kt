package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.ChildAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.animation.page.PageArrangement

internal val defaultChildAnimationSpec: FiniteAnimationSpec<Float> = tween()

@ExperimentalDecomposeApi
fun <C : Any, T : Any> slideFromBottom(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): ChildAnimation<C, T> =
    childAnimation(animationSpec = animationSpec) { _, factor, arrangement, _, content ->
        content(
            Modifier.offset(
                y = when (arrangement) {
                    PageArrangement.PREVIOUS -> 0.dp
                    PageArrangement.FOLLOWING -> maxHeight * (1F - factor)
                }
            )
        )
    }

@ExperimentalDecomposeApi
fun <C : Any, T : Any> slideFromRight(
    animationSpec: FiniteAnimationSpec<Float> = defaultChildAnimationSpec,
): ChildAnimation<C, T> =
    childAnimation(animationSpec = animationSpec) { _, factor, arrangement, _, content ->
        content(
            Modifier.offset(
                x = when (arrangement) {
                    PageArrangement.PREVIOUS -> 0.dp
                    PageArrangement.FOLLOWING -> maxWidth * (1F - factor)
                }
            )
        )
    }