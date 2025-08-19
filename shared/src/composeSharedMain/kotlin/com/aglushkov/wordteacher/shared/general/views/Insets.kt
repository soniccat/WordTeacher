package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PageSize.Fill.calculateMainAxisPageSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.windowInsetsHorizontalPadding() = windowInsetsPadding(
WindowInsets.displayCutout
            .union(WindowInsets.systemBars)
            .only(WindowInsetsSides.Horizontal)
)

@Composable
fun Modifier.windowInsetsRightPadding() = windowInsetsPadding(
    WindowInsets.displayCutout
        .union(WindowInsets.systemBars)
        .only(WindowInsetsSides.Right)
)

@Composable
fun Modifier.windowInsetsVerticalPadding() = windowInsetsPadding(
    WindowInsets.displayCutout
        .union(WindowInsets.systemBars)
        .only(WindowInsetsSides.Vertical)
)

@Composable
fun Modifier.windowInsetsVerticalPaddingWithIME() = windowInsetsPadding(
    WindowInsets.safeDrawing
        .only(WindowInsetsSides.Vertical)
)