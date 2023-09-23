package com.aglushkov.wordteacher.shared.features.settings.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.features.settings.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsUI(
    vm: SettingsVM,
    modifier: Modifier = Modifier,
) {
    val itemsState = vm.items.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(MR.strings.settings_title)) },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 300.dp
                )
            ) {
                items(itemsState.value, key = { it.id }) { item ->
                    showSettingsItem(
                        Modifier.animateItemPlacement(),
                        item,
                        vm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showSettingsItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: SettingsVM,
) = when (item) {
    is SettingsViewTitleItem -> {
        ListItem (
            text = { Text(item.firstItem().localized()) }
        )
    }
    is SettingsViewLoading -> {
        val side = 30.dp
        Box(
            modifier = Modifier.size(side, side)
        ) {
            CircularProgressIndicator(
                color = Color.LightGray.copy(alpha = 0.2f)
            )
        }
    }
    is SettingsViewAuthButtonItem -> {
        Button(onClick = { vm.onAuthButtonClicked(item.buttonType) }) {
            Text(text = item.firstItem().localized())
        }
    }
    is SettingsViewAuthRefreshButtonItem -> {
        Button(onClick = { vm.onAuthRefreshClicked() }) {
            Text(text = item.firstItem().localized())
        }
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}
