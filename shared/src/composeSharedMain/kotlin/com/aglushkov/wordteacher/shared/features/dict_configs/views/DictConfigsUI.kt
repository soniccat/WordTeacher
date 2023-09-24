package com.aglushkov.wordteacher.shared.features.dict_configs.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Checkbox
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
            .background(color = MaterialTheme.colors.background)
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
        ConfigTextField(
            modifier = modifier.padding(
                horizontal = LocalDimensWord.current.wordHorizontalPadding
            ),
            initialText = "",
            placeholder = if (item.hasToken) {
                "type to change api token"
            } else {
                "api token"
            }
        ) {
            vm.onYandexConfigChanged(item, it, null, null)
        }
        ConfigTextField(
            modifier = modifier.padding(
                horizontal = LocalDimensWord.current.wordHorizontalPadding
            ),
            initialText = item.lang,
            placeholder = if (item.lang.isEmpty()) {
                "language: en-en, en-ru ..."
            } else {
                ""
            }
        ) {
            vm.onYandexConfigChanged(item, null, it, null)
        }
        ListItem(
            modifier = Modifier.clickable {
                vm.onYandexConfigChanged(item, null, null, item.settings.copy(
                    applyFamilySearchFilter = !item.settings.applyFamilySearchFilter
                ))
            },
            trailing = {
                Checkbox(
                    checked = item.settings.applyFamilySearchFilter,
                    onCheckedChange = null
                )
            },
            text = { Text("Apply the family search filter") },
        )
        ListItem(
            modifier = Modifier.clickable {
                vm.onYandexConfigChanged(item, null, null, item.settings.copy(
                    enableSearchingByWordForm = !item.settings.enableSearchingByWordForm
                ))
            },
            trailing = {
                Checkbox(
                    checked = item.settings.enableSearchingByWordForm,
                    onCheckedChange = null
                )
            },
            text = { Text("Enable searching by word form") },
        )
        ListItem(
            modifier = Modifier.clickable {
                vm.onYandexConfigChanged(item, null, null, item.settings.copy(
                    enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation = !item.settings.enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation
                ))
            },
            trailing = {
                Checkbox(
                    checked = item.settings.enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation,
                    onCheckedChange = null
                )
            },
            text = { Text("Enable a filter that requires matching parts of speech for the search word and translation") },
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

@Composable
private fun ConfigTextField(
    modifier: Modifier = Modifier,
    initialText: String,
    placeholder: String = "",
    onItemChanged: (text: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    InlineTextField(
        modifier = modifier,
        value = textState,
        placeholder = placeholder,
        textStyle = LocalAppTypography.current.wordDefinitionTranscripton.copy(
            color = LocalContentColor.current
        ),
        focusManager = focusManager,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onValueChange = {
            textState = it
            onItemChanged(it.text)
        }
    )
}
