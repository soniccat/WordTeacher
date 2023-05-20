package com.aglushkov.wordteacher.shared.general

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

@Composable
actual fun DropdownMenu(expanded: Boolean, onDismissRequest: ()->Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = content,
    )
}

@Composable
actual fun DropdownMenuItem(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material.DropdownMenuItem(
        onClick = onClick,
        content = content
    )
}