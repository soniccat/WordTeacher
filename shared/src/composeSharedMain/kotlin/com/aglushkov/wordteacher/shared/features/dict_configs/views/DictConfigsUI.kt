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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigCreateViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigDictViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigHeaderViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigLoadingViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigTextViewItem
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVM
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.ConfigYandexViewItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTextItem
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsViewTitleItem
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resources.resolve
import com.aglushkov.wordteacher.shared.general.views.CustomListItem
import com.aglushkov.wordteacher.shared.general.views.DeletableCell
import com.aglushkov.wordteacher.shared.general.views.InlineTextField
import com.aglushkov.wordteacher.shared.general.views.LoadingViewItemUI
import com.aglushkov.wordteacher.shared.general.views.listBottomPadding
import com.aglushkov.wordteacher.shared.general.views.windowInsetsHorizontalPadding
import wordteacher.shared.generated.resources.Res
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import wordteacher.shared.generated.resources.arrow_back_24
import wordteacher.shared.generated.resources.create_note
import wordteacher.shared.generated.resources.dictconfigs_word_count
import wordteacher.shared.generated.resources.dictconfigs_yandex_api_token_doesnt_exist_placeholder
import wordteacher.shared.generated.resources.dictconfigs_yandex_api_token_exists_placeholder
import wordteacher.shared.generated.resources.dictconfigs_yandex_language_placeholder
import wordteacher.shared.generated.resources.dictconfigs_yandex_param_family_search_filter
import wordteacher.shared.generated.resources.dictconfigs_yandex_param_require_matching_parts
import wordteacher.shared.generated.resources.dictconfigs_yandex_param_search_by_word_form
import wordteacher.shared.generated.resources.dictconfigs_yandex_title
import wordteacher.shared.generated.resources.settings_title

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
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_back_24),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().windowInsetsHorizontalPadding(),
                contentPadding = PaddingValues(
                    bottom = listBottomPadding()
                )
            ) {
                items(viewItems.value, key = { it.id }) { item ->
                    showDictConfigsItem(
                        Modifier.animateItem(),
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
    is ConfigYandexViewItem -> {
        YandexConfigViewItemUI(item, vm, modifier)
    }
    is ConfigCreateViewItem -> {
        AddConfigItemUI(
            vm,
            item.firstItem(),
            modifier,
        )
    }
    is ConfigHeaderViewItem -> {
        ListItem (
            text = { Text(item.firstItem().resolve(), style = LocalAppTypography.current.settingsTitle) }
        )
    }
    is ConfigTextViewItem -> {
        CustomListItem (
            Modifier.padding(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                bottom = LocalDimens.current.contentPadding
            ),
            content = { Text(item.firstItem().resolve(), style = LocalAppTypography.current.settingsText) }
        )
    }
    is ConfigDictViewItem -> {
        DeletableCell(
            stateKey = item.id,
            onDeleted = {
                vm.onDictDeleted(item)
            }
        ) {
            CustomListItem(
                secondaryContent = {
                    Text(stringResource(Res.string.dictconfigs_word_count, item.wordCount ?: 0), style = LocalAppTypography.current.listItemSubtitle)
                }
            ) {
                Text(item.firstItem(), style = LocalAppTypography.current.settingsText)
            }

        }
    }
    is ConfigLoadingViewItem -> {
        LoadingViewItemUI()
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun YandexConfigViewItemUI(
    item: ConfigYandexViewItem,
    vm: DictConfigsVM,
    modifier: Modifier
) {
    DeletableCell(
        Modifier.padding(horizontal = LocalDimens.current.contentPadding),
        stateKey = item.id,
        onDeleted = {
            vm.onConfigDeleteClicked(item)
        }
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.dictconfigs_yandex_title),
                modifier = Modifier.padding(vertical = LocalDimens.current.contentPadding),
                style = LocalAppTypography.current.dictConfigTitle
            )
            ConfigTextField(
                modifier = Modifier.padding(bottom = LocalDimens.current.contentPadding),
                initialText = "",
                placeholder = if (item.hasToken) {
                    stringResource(Res.string.dictconfigs_yandex_api_token_exists_placeholder)
                } else {
                    stringResource(Res.string.dictconfigs_yandex_api_token_doesnt_exist_placeholder)
                }
            ) {
                vm.onYandexConfigChanged(item, it, null, null)
            }
            // TODO: get list of available dictionaries from yandex api
            ConfigTextField(
                modifier = Modifier.padding(bottom = LocalDimens.current.contentPadding),
                initialText = item.lang,
                placeholder = if (item.lang.isEmpty()) {
                    stringResource(Res.string.dictconfigs_yandex_language_placeholder)
                } else {
                    ""
                }
            ) {
                vm.onYandexConfigChanged(item, null, it, null)
            }
            CustomListItem(
                modifier = Modifier.clickable {
                        vm.onYandexConfigChanged(
                            item, null, null, item.settings.copy(
                                applyFamilySearchFilter = !item.settings.applyFamilySearchFilter
                            )
                        )
                    },
                contentPadding = PaddingValues(bottom = LocalDimens.current.contentPadding),
                trailing = {
                    Checkbox(
                        checked = item.settings.applyFamilySearchFilter,
                        onCheckedChange = null
                    )
                },
                content = { Text(stringResource(Res.string.dictconfigs_yandex_param_family_search_filter)) },
            )
            CustomListItem(
                modifier = Modifier.clickable {
                    vm.onYandexConfigChanged(
                        item, null, null, item.settings.copy(
                            enableSearchingByWordForm = !item.settings.enableSearchingByWordForm
                        )
                    )
                },
                contentPadding = PaddingValues(bottom = LocalDimens.current.contentPadding),
                trailing = {
                    Checkbox(
                        checked = item.settings.enableSearchingByWordForm,
                        onCheckedChange = null
                    )
                },
                content = { Text(stringResource(Res.string.dictconfigs_yandex_param_search_by_word_form)) },
            )
            CustomListItem(
                modifier = Modifier
                    .clickable {
                        vm.onYandexConfigChanged(
                            item, null, null, item.settings.copy(
                                enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation = !item.settings.enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation
                            )
                        )
                    },
                contentPadding = PaddingValues(bottom = LocalDimens.current.contentPadding),
                trailing = {
                    Checkbox(
                        checked = item.settings.enableFilterThatRequiresMatchingPartsOfSpeechForSearchWordAndTranslation,
                        onCheckedChange = null
                    )
                },
                content = { Text(stringResource(Res.string.dictconfigs_yandex_param_require_matching_parts)) },
            )
        }
    }
}

@Composable
private fun AddConfigItemUI(
    vm: DictConfigsVM,
    type: ConfigCreateViewItem.Type,
    modifier: Modifier,
) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            IconButton(
                onClick = {
                    vm.onConfigAddClicked(type)
                }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.create_note),
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
    val textState = rememberTextFieldState(initialText)

    LaunchedEffect(textState) {
        snapshotFlow { textState.text }
            .drop(1)
            .collectLatest { text ->
                onItemChanged(text.toString())
            }
    }

    InlineTextField(
        modifier = modifier,
        state = textState,
        placeholder = placeholder,
        textStyle = LocalAppTypography.current.dictParamText.copy(
            color = LocalContentColor.current
        ),
        focusManager = focusManager,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}
