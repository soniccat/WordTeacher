package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.windowInsetsHorizontalPadding() = windowInsetsPadding(
WindowInsets.displayCutout
            .union(WindowInsets.systemBars)
            .only(WindowInsetsSides.Horizontal)
)

@Composable
fun Modifier.windowInsetsVerticalPadding() = windowInsetsPadding(
    WindowInsets.displayCutout
        .union(WindowInsets.systemBars)
        .only(WindowInsetsSides.Vertical)
)

@Composable
fun Modifier.windowInsetsVerticalPaddingWithIME() = windowInsetsPadding(
    WindowInsets.displayCutout
        .union(WindowInsets.systemBars)
        .union(WindowInsets.ime)
        .only(WindowInsetsSides.Vertical)
)