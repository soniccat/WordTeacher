package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun ChipPreview() {
    Column(
    ) {
        Chip(
            modifier = Modifier.padding(4.dp),
            text = "By Source",
            isChecked = true,
            colors = null,
            isCloseIconVisible = true,
            closeBlock = {},
            clickBlock = {}
        )
        Chip(
            modifier = Modifier.padding(4.dp),
            text = "By Source",
            isChecked = false,
            colors = null,
            isCloseIconVisible = true,
            closeBlock = {},
            clickBlock = {}
        )
        Chip(
            modifier = Modifier.padding(4.dp),
            text = "Add Filter",
            isChecked = false,
            colors = ChipColors(
                contentColor = MaterialTheme.colors.onSecondary,
                bgColor = MaterialTheme.colors.secondary
            ),
            isCloseIconVisible = false,
            closeBlock = {},
            clickBlock = {}
        )
        Chip(
            modifier = Modifier.padding(4.dp),
            text = "Add Filter",
            isChecked = false,
            colors = ChipColors(
                contentColor = MaterialTheme.colors.onSecondary,
                bgColor = MaterialTheme.colors.secondary
            ),
            isCloseIconVisible = true,
            closeBlock = {},
            clickBlock = {}
        )
    }
}
