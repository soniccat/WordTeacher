package com.aglushkov.wordteacher.shared.general

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimens(
    val contentPadding: Dp = 16.dp,
    val halfOfContentPadding: Dp = 8.dp,
    val indentSmall: Dp = contentPadding,

    val definitionsSearchHorizontalMargin: Dp = contentPadding,
    val definitionsSearchVerticalMargin: Dp = 8.dp,
    val definitionsDisplayModeHorizontalPadding: Dp = definitionsSearchHorizontalMargin,
    val definitionsDisplayModeVerticalPadding: Dp = contentPadding,

    val learningTestOptionMargin: Dp = 8.dp
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
