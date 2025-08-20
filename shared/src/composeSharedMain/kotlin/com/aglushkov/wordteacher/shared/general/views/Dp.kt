package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Stable
@Composable
fun Float.pxToDp(): Dp = Dp(this / LocalDensity.current.density)

@Stable
@Composable
fun Int.pxToDp(): Dp = Dp(this / LocalDensity.current.density)

@Stable
@Composable
fun Int.dpToPx(): Float = this * LocalDensity.current.density

@Composable
fun listBottomPadding(): Dp {
    val px = LocalWindowInfo.current.containerSize.height / 4.0f
    return px.pxToDp()
}