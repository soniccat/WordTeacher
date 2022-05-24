package com.aglushkov.wordteacher.androidApp.features.definitions.views

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.toDp
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserUI
import com.aglushkov.wordteacher.androidApp.general.views.chooser_dialog.ChooserViewItem
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.androidApp.general.views.compose.ChipColors
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@ExperimentalFoundationApi
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DefinitionsUI(
    vm: DefinitionsVM,
    contentModifier: Modifier = Modifier,
    modalModifier: Modifier = Modifier,
    withSearchBar: Boolean = true,
    contentHeader: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val partsOfSpeech by vm.partsOfSpeechFilterStateFlow.collectAsState()
    val selectedPartsOfSpeeches by vm.selectedPartsOfSpeechStateFlow.collectAsState()
    val partOfSpeechFilterBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val focusManager = LocalFocusManager.current

    // TODO: consider moving chooser outside...
    ChooserUI(
        state = partOfSpeechFilterBottomSheetState,
        items = partsOfSpeech.map { partOfSpeech ->
            val isSelected = selectedPartsOfSpeeches.contains(partOfSpeech)
            ChooserViewItem(0, partOfSpeech.name, partOfSpeech, isSelected)
        },
        modifier = modalModifier,
        onSelected = { items ->
            vm.onPartOfSpeechFilterUpdated(
                items.filter { option ->
                    option.isSelected
                }.map { option ->
                    option.obj as WordTeacherWord.PartOfSpeech
                }
            )
        }
    ) {
        DefinitionsWordUI(
            vm,
            contentModifier,
            withSearchBar,
            contentHeader,
            onPartOfSpeechFilterClicked = { items ->
                focusManager.clearFocus() // consider showing choose in a window popup
                scope.launch {
                    partOfSpeechFilterBottomSheetState.show()
                }
            }
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun DefinitionsWordUI(
    vm: DefinitionsVM,
    modifier: Modifier = Modifier,
    withSearchBar: Boolean,
    contentHeader: @Composable () -> Unit,
    onPartOfSpeechFilterClicked: (item: DefinitionsDisplayModeViewItem) -> Unit
) {
    val defs = vm.definitions.collectAsState()
    var searchText by remember { mutableStateOf(vm.state.word.orEmpty()) }
    var needShowSuggests by remember { mutableStateOf(false) }
    val suggests = vm.suggests.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    if (withSearchBar) {
        BackHandler(enabled = needShowSuggests) {
            focusManager.clearFocus()
        }
    }

    val isNotEmpty by remember(defs) {
        derivedStateOf {
            defs.value.data()?.isNotEmpty() == true
        }
    }
    val derivedDefs by remember(defs) {
        derivedStateOf {
            val data = defs.value.data() ?: emptyList()
            data
        }
    }
    val defsValue by remember(defs) {
        derivedStateOf {
            defs.value
        }
    }

    Column(
        modifier = modifier,
    ) {
        if (withSearchBar) {
            CustomTopAppBar {
                SearchView(
                    searchText,
                    focusRequester = focusRequester,
                    onTextChanged = {
                        searchText = it

                        if (it.isEmpty()) {
                            vm.clearSuggests()
                        } else {
                            vm.requestSuggests(it)
                        }
                    },
                    onFocusChanged = {
                        needShowSuggests = it.isFocused
                    }
                ) {
                    vm.onWordSubmitted(searchText)
                    focusManager.clearFocus()
                }
            }
        }

        contentHeader()

        val suggestsRes = suggests.value
        val suggestsData = suggestsRes.data()
        if (needShowSuggests && suggestsData?.isNotEmpty() == true) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 300.dp
                )
            ) {
                items(suggestsData, key = { it.id }) { item ->
                    showSuggestItem(
                        Modifier.animateItemPlacement(),
                        item,
                        vm,
                        onClicked = {
                            vm.onWordSubmitted(it.firstItem())
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        } else {
            if (isNotEmpty) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = 300.dp
                    )
                ) {
                    items(derivedDefs, key = { it.id }, contentType = { it.type }) { item ->
                        showViewItem(
                            Modifier.animateItemPlacement(),
                            item,
                            vm,
                            onPartOfSpeechFilterClicked,
                            { vm.onPartOfSpeechFilterCloseClicked(it) },
                            { vm.onDisplayModeChanged(it) }
                        )
                    }
                }
            } else {
                LoadingStatusView(
                    resource = defsValue,
                    loadingText = null,
                    errorText = vm.getErrorText(defsValue)?.resolveString(),
                    emptyText = LocalContext.current.getString(R.string.error_no_definitions)
                ) {
                    vm.onTryAgainClicked()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showSuggestItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DefinitionsVM,
    onClicked: (item: WordSuggestViewItem) -> Unit
) = when (item) {
    is WordSuggestViewItem -> {
        ListItem (
            modifier = modifier
                .clickable { onClicked.invoke(item) },
            secondaryText = { Text(item.source) },
            text = { Text(item.firstItem()) }
        )
    }
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}

@Composable
private fun showViewItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DefinitionsVM,
    onPartOfSpeechFilterClicked: (DefinitionsDisplayModeViewItem) -> Unit,
    onPartOfSpeechFilterCloseClicked: (DefinitionsDisplayModeViewItem) -> Unit,
    onDisplayModeChanged: (DefinitionsDisplayMode) -> Unit,
) = when (item) {
    is DefinitionsDisplayModeViewItem -> DefinitionsDisplayModeView(
        item,
        modifier,
        { onPartOfSpeechFilterClicked(item) },
        { onPartOfSpeechFilterCloseClicked(item) },
        onDisplayModeChanged
    )
    is WordDividerViewItem -> WordDividerView(modifier)
    is WordTitleViewItem -> WordTitleView(item, modifier)
    is WordTranscriptionViewItem -> WordTranscriptionView(item, modifier)
    is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item, modifier)
    is WordDefinitionViewItem -> WordDefinitionView(
        item,
        modifier,
        textContent = { text, ts ->
            Text(
                modifier = Modifier.weight(1.0f),
                text = text,
                style = ts
            )
            AddToSet(vm, item)
        }
    )
    is WordSubHeaderViewItem -> WordSubHeaderView(item, modifier)
    is WordSynonymViewItem -> WordSynonymView(item, modifier)
    is WordExampleViewItem -> WordExampleView(item, modifier)
    else -> {
        Text(
            text = "unknown item $item",
            modifier = modifier
        )
    }
}

@Composable
private fun AddToSet(vm: DefinitionsVM, wordDefinitionViewItem: WordDefinitionViewItem) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        AddIcon { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val sets = vm.cardSets.collectAsState()

            if (sets.value.isLoading()) {
                CircularProgressIndicator()
            }

            sets.value.data()?.let { items ->
                items.onEach {
                    when (it) {
                        is CardSetViewItem -> DropdownMenuItem(
                            onClick = {
                                vm.onAddDefinitionInSet(wordDefinitionViewItem, it)
                                expanded = false
                            }
                        ) {
                            Text(it.name)
                        }
                        is OpenCardSetViewItem -> DropdownMenuItem(
                            onClick = {
                                vm.onOpenCardSets(it)
                                expanded = false
                            }
                        ) {
                            Text(it.text.toString(LocalContext.current))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefinitionsDisplayModeView(
    item: DefinitionsDisplayModeViewItem,
    modifier: Modifier,
    onPartOfSpeechFilterClicked: () -> Unit,
    onPartOfSpeechFilterCloseClicked: () -> Unit,
    onDisplayModeChanged: (mode: DefinitionsDisplayMode) -> Unit
) {
    val horizontalPadding = dimensionResource(R.dimen.definitions_displayMode_horizontal_padding)
    val topPadding = dimensionResource(R.dimen.definitions_displayMode_vertical_padding)
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding
            )
            .horizontalScroll(rememberScrollState())
    ) {
        Chip(
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 4.dp
            ),
            text = item.partsOfSpeechFilterText.resolveString(),
            colors = ChipColors(
                contentColor = MaterialTheme.colors.onSecondary,
                bgColor = MaterialTheme.colors.secondary
            ),
            isCloseIconVisible = item.canClearPartsOfSpeechFilter,
            closeBlock = {
                onPartOfSpeechFilterCloseClicked()
            },
            clickBlock = {
                onPartOfSpeechFilterClicked()
            }
        )

        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.definitions_displayMode_horizontal_padding)))

        // Group
        val selectedMode = item.items[item.selectedIndex]
        val firstMode = item.items.firstOrNull()
        for (mode in item.items) {
            Chip(
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = 4.dp,
                    start = if (mode == firstMode) 0.dp else 4.dp,
                    end = 4.dp
                ),
                text = mode.toStringDesc().resolveString(),
                isChecked = mode == selectedMode
            ) {
                onDisplayModeChanged(mode)
            }
        }
    }
}

@Composable
fun WordDividerView(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = Modifier
            .then(modifier)
            .padding(
                top = dimensionResource(id = R.dimen.word_divider_topMargin),
                bottom = dimensionResource(id = R.dimen.word_divider_bottomMargin)
            )
    )
}

@Composable
fun WordTitleView(
    viewItem: WordTitleViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTypography.wordDefinitionTitle,
    textContent: @Composable RowScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            modifier = Modifier
                .weight(1.0f, true),
            style = ts
        )
    }
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding)
            )
    ) {
        textContent(viewItem.firstItem(), textStyle)
        if (viewItem.providers.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.word_providedBy_template,
                    viewItem.providers.joinToString()
                ),
                modifier = Modifier
                    .widthIn(max = dimensionResource(id = R.dimen.word_providedBy_maxWidth)),
                textAlign = TextAlign.End,
                style = AppTypography.wordDefinitionProvidedBy
            )
        }
    }
}

@Composable
fun WordTranscriptionView(
    viewItem: WordTranscriptionViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTypography.wordDefinitionTranscripton,
    textContent: @Composable BoxScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            style = ts
        )
    }
) {
    Box(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding)
            )
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

@Composable
fun WordPartOfSpeechView(
    viewItem: WordPartOfSpeechViewItem,
    modifier: Modifier = Modifier,
    topPadding: Dp = dimensionResource(id = R.dimen.word_partOfSpeech_topMargin)
) {
    Text(
        text = viewItem.firstItem().resolveString().toUpperCase(Locale.getDefault()),
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = topPadding
            ),
        style = AppTypography.wordDefinitionPartOfSpeech
    )
}

@Composable
fun WordDefinitionView(
    viewItem: WordDefinitionViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTypography.wordDefinition,
    textContent: @Composable RowScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            style = ts
        )
    }
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = dimensionResource(id = R.dimen.word_header_topMargin)
            ),
    ) {
        Text(" • ")
        textContent(viewItem.firstItem(), textStyle)
    }
}

@Composable
fun WordSubHeaderView(
    viewItem: WordSubHeaderViewItem,
    modifier: Modifier = Modifier,
    textContent: @Composable RowScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            style = ts
        )
    }
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding) + viewItem.indent.toDp(),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = dimensionResource(id = R.dimen.word_subHeader_topMargin)
            ),
    ) {
        textContent(viewItem.firstItem().resolveString(), AppTypography.wordDefinitionSubHeader)
    }
}

@Composable
fun WordSynonymView(
    viewItem: WordSynonymViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTypography.wordSynonym,
    textContent: @Composable RowScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            style = ts
        )
    }
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding) + viewItem.indent.toDp(),
                end = dimensionResource(id = R.dimen.word_horizontalPadding)
            ),
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

@Composable
fun WordExampleView(
    viewItem: WordExampleViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTypography.wordExample,
    textContent: @Composable RowScope.(text: String, textStyle: TextStyle) -> Unit = { text, ts ->
        Text(
            text = text,
            style = ts
        )
    }
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding) + viewItem.indent.toDp(),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
//                top = dimensionResource(id = R.dimen.word_header_topMargin)
            ),
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

// Previews

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewWithResponse() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(
                Resource.Loaded(
                    listOf(
                        DefinitionsDisplayModeViewItem(
                            partsOfSpeechFilterText = StringDesc.Raw("Noun"),
                            canClearPartsOfSpeechFilter = true,
                            modes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged),
                            selectedIndex = 0
                        ),
                        WordDividerViewItem(),
                        WordTitleViewItem(
                            title = "Word",
                            providers = listOf(Config.Type.Yandex)
                        ),
                        WordTitleViewItem(
                            title = "Word 2",
                            providers = listOf(Config.Type.Yandex, Config.Type.Google, Config.Type.OwlBot)
                        ),
                        WordTranscriptionViewItem("[omg]"),
                        WordPartOfSpeechViewItem(StringDesc.Raw("Noun"), WordTeacherWord.PartOfSpeech.Noun),
                        WordDefinitionViewItem("* definition 1"),
                        WordDefinitionViewItem("* definition 2"),
                        WordSynonymViewItem("synonym 1", Indent.NONE),
                        WordSynonymViewItem("synonym 2", Indent.SMALL),
                        WordExampleViewItem("example 1", Indent.NONE),
                        WordExampleViewItem("example 2", Indent.SMALL),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 1"), Indent.NONE),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 2"), Indent.SMALL),
                    )
                )
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewLoading() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(Resource.Loading())
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun DefinitionsUIPreviewError() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(
                Resource.Error(
                    IOException("Sth went wrong"),
                    true
                )
            )
        )
    }
}
