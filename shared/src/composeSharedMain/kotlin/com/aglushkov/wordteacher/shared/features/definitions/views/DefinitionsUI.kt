package com.aglushkov.wordteacher.shared.features.definitions.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.general.views.CustomTopAppBar
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.SearchView
import com.aglushkov.wordteacher.shared.general.views.chooser_dialog.ChooserUI
import com.aglushkov.wordteacher.shared.general.views.chooser_dialog.ChooserViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.format
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

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

@OptIn(ExperimentalFoundationApi::class)
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
// TODO:
//        BackHandler(enabled = needShowSuggests) {
//            focusManager.clearFocus()
//        }
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
                    Modifier,
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
                    errorText = vm.getErrorText(defsValue)?.localized(),
                    emptyText = stringResource(MR.strings.error_no_definitions)
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
    is WordLoadingViewItem -> {
        Box(Modifier.fillMaxWidth().padding(LocalDimens.current.contentPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
                            Text(it.text.localized())
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
    val horizontalPadding = LocalDimens.current.definitionsDisplayModeHorizontalPadding
    val topPadding = LocalDimens.current.definitionsDisplayModeVerticalPadding
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
        com.aglushkov.wordteacher.shared.general.views.Chip(
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 4.dp
            ),
            text = item.partsOfSpeechFilterText.localized(),
            colors = com.aglushkov.wordteacher.shared.general.views.ChipColors(
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

        Spacer(modifier = Modifier.width(LocalDimens.current.definitionsDisplayModeHorizontalPadding))

        // Group
        val selectedMode = item.items[item.selectedIndex]
        val firstMode = item.items.firstOrNull()
        for (mode in item.items) {
            com.aglushkov.wordteacher.shared.general.views.Chip(
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = 4.dp,
                    start = if (mode == firstMode) 0.dp else 4.dp,
                    end = 4.dp
                ),
                text = mode.toStringDesc().localized(),
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
                top = LocalDimensWord.current.wordDividerTopMargin,
                bottom = LocalDimensWord.current.wordDividerBottomMargin
            )
    )
}

@Composable
fun WordTitleView(
    viewItem: WordTitleViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalAppTypography.current.wordDefinitionTitle,
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
                start = LocalDimensWord.current.wordHorizontalPadding,
                end = LocalDimensWord.current.wordHorizontalPadding
            )
    ) {
        textContent(viewItem.firstItem(), textStyle)
        if (viewItem.providers.isNotEmpty()) {
            Text(
                text = MR.strings.word_providedBy_template.format(viewItem.providers.joinToString()).localized(),
                modifier = Modifier
                    .widthIn(max = LocalDimensWord.current.wordProvidedByMaxWidth),
                textAlign = TextAlign.End,
                style = LocalAppTypography.current.wordDefinitionProvidedBy
            )
        }
    }
}

@Composable
fun WordTranscriptionView(
    viewItem: WordTranscriptionViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalAppTypography.current.wordDefinitionTranscripton,
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
                start = LocalDimensWord.current.wordHorizontalPadding,
                end = LocalDimensWord.current.wordHorizontalPadding
            )
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

@Composable
fun WordPartOfSpeechView(
    viewItem: WordPartOfSpeechViewItem,
    modifier: Modifier = Modifier,
    topPadding: Dp = LocalDimensWord.current.wordPartOfSpeechTopMargin
) {
    Text(
        text = viewItem.firstItem().localized().uppercase(Locale.getDefault()),
        modifier = Modifier
            .then(modifier)
            .padding(
                start = LocalDimensWord.current.wordHorizontalPadding,
                end = LocalDimensWord.current.wordHorizontalPadding,
                top = topPadding
            ),
        style = LocalAppTypography.current.wordDefinitionPartOfSpeech
    )
}

@Composable
fun WordDefinitionView(
    viewItem: WordDefinitionViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalAppTypography.current.wordDefinition,
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
                start = LocalDimensWord.current.wordHorizontalPadding,
                end = LocalDimensWord.current.wordHorizontalPadding,
                top = LocalDimensWord.current.wordHeaderTopMargin
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
                start = LocalDimensWord.current.wordHorizontalPadding + viewItem.indent.toDp(),
                end = LocalDimensWord.current.wordHorizontalPadding,
                top = LocalDimensWord.current.wordSubHeaderTopMargin
            ),
    ) {
        textContent(viewItem.firstItem().localized(), LocalAppTypography.current.wordDefinitionSubHeader)
    }
}

@Composable
fun WordSynonymView(
    viewItem: WordSynonymViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalAppTypography.current.wordSynonym,
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
                start = LocalDimensWord.current.wordHorizontalPadding + viewItem.indent.toDp(),
                end = LocalDimensWord.current.wordHorizontalPadding
            ),
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

@Composable
fun WordExampleView(
    viewItem: WordExampleViewItem,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalAppTypography.current.wordExample,
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
                start = LocalDimensWord.current.wordHorizontalPadding + viewItem.indent.toDp(),
                end = LocalDimensWord.current.wordHorizontalPadding,
//                top = dimensionResource(id = R.dimen.word_header_topMargin)
            ),
    ) {
        textContent(viewItem.firstItem(), textStyle)
    }
}

