package com.aglushkov.wordteacher.shared.general

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.format

@Composable
expect fun DropdownMenu(expanded: Boolean, onDismissRequest: ()->Unit, content: @Composable ColumnScope.() -> Unit)

@Composable
expect fun DropdownMenuItem(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
)