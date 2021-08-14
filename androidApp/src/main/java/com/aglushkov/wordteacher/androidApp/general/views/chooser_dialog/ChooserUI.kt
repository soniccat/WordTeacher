package com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@ExperimentalMaterialApi
@Composable
fun ChooserUI(
    state: ModalBottomSheetState,
    items: List<ChooserViewItem>,
    modifier: Modifier = Modifier,
    onSelected: (selected: List<ChooserViewItem>) -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = state,
        sheetContent = {
            Column(
                // We must keep it non empty to avoid this crash: https://kotlinlang.slack.com/archives/CJLTWPH7S/p1619607891021400
                modifier = Modifier.padding(top = 16.dp)
            ) {
                LazyColumn{
                    items(items) { item ->
                        ListItem(
                            modifier = Modifier.clickable {
                                item.isSelected = !item.isSelected
                                onSelected(
                                    items.filter { it.isSelected }
                                )
                            },
                            trailing = {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = null
                                )
                            },
                            text = { Text(item.name) },
                        )
                    }
                }
            }
        },
        modifier = modifier,
        content = content
    )
}