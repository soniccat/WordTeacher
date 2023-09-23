package com.aglushkov.wordteacher.shared.features.dict_configs.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.CreateConfigViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVM
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.YandexConfigViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.InlineTextField
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DictConfigsUI(
    vm: DictConfigsVM,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    val viewItems = vm.viewItems.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(MR.strings.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(
                            painter = painterResource(MR.images.arrow_back_24),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 300.dp
                )
            ) {
                items(viewItems.value, key = { it.id }) { item ->
                    showDictConfigsItem(
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
private fun showDictConfigsItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DictConfigsVM,
) = when (item) {
    is YandexConfigViewItem -> {
        Text(
            "Yandex",
            modifier = modifier.padding(
                horizontal = LocalDimensWord.current.wordHorizontalPadding
            ),
            style = LocalAppTypography.current.wordDefinitionTitle
        )
    }
    is CreateConfigViewItem -> {
        CreateYandexConfigView(
            modifier
        ) {
            vm.onYandexConfigAddClicked()
        }
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}

@Composable
private fun CreateYandexConfigView(
    modifier: Modifier,
    onClicked: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        IconButton(
            onClick = onClicked
        ) {
            Icon(
                painter = painterResource(MR.images.create_note),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ConfigTextField(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    item: BaseViewItem<*>,
    cardId: Long,
    vm: CardSetVM
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    InlineTextField(
        modifier = modifier.onPreviewKeyEvent {
            if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                focusManager.moveFocus(FocusDirection.Down)
                true
            } else {
                false
            }
        },
        value = textState,
        placeholder = vm.getPlaceholder(item)?.localized().orEmpty(),
        textStyle = textStyle.copy(
            color = LocalContentColor.current
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ),
        onValueChange = {
            textState = it
            vm.onItemTextChanged(it.text, item, cardId)
        }
    )
}
