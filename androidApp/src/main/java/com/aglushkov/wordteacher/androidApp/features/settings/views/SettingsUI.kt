package com.aglushkov.wordteacher.androidApp.features.settings.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomDialogUI
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSuggestViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewAuthButtonItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewLoading
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@Composable
fun SettingsUI(
    vm: SettingsVM,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val itemsState = vm.items.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
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
            text = { Text(item.firstItem().toString(LocalContext.current)) }
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
            Text(text = item.firstItem().toString(LocalContext.current))
        }
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}
