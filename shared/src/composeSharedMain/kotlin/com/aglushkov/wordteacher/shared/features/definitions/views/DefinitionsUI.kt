@file:OptIn(ExperimentalLayoutApi::class)

package com.aglushkov.wordteacher.shared.features.definitions.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.shared.features.add_article.views.CustomSnackbar
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetExpandOrCollapseViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.general.views.CustomTopAppBar
import com.aglushkov.wordteacher.shared.general.views.ListSectionCell
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.SearchView
import com.aglushkov.wordteacher.shared.general.views.chooser_dialog.ChooserUI
import com.aglushkov.wordteacher.shared.general.views.chooser_dialog.ChooserViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.format
import kotlinx.coroutines.launch
import java.util.*
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.painterResource

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
    val partOfSpeechFilterBottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val focusManager = LocalFocusManager.current
    val events by vm.events.collectAsState()
    val eventToShow by remember(events) {
        derivedStateOf {
            vm.events.value.cardSetUpdatedEvents.firstOrNull()
        }
    }

    Box(modifier = modalModifier.fillMaxSize()) {
        // TODO: consider moving chooser outside...
        ChooserUI(
            state = partOfSpeechFilterBottomSheetState,
            items = partsOfSpeech.map { partOfSpeech ->
                val isSelected = selectedPartsOfSpeeches.contains(partOfSpeech)
                ChooserViewItem(0, partOfSpeech.name, partOfSpeech, isSelected)
            },
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

        val snackbarHostState = remember { SnackbarHostState() }
        val snackBarMessage = eventToShow?.text?.localized().orEmpty()
        val snackBarActionText = eventToShow?.actionText?.localized().orEmpty()
        LaunchedEffect(eventToShow) {
            eventToShow?.let { event ->
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        snackBarMessage,
                        snackBarActionText
                    )
                    vm.onEventHandled(event, result == SnackbarResult.ActionPerformed)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.Companion.align(Alignment.BottomCenter)
        ) {
            CustomSnackbar(
                message = null,
                snackbarData = it,
            )
        }
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
    val searchText = vm.wordTextValue.collectAsState()
    var needShowSuggests by remember { mutableStateOf(false) }
    val suggests = vm.suggests.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val needShowWordHistory by vm.isWordHistorySelected.collectAsState()
    val wordStack by vm.wordStack.collectAsState()

    if (withSearchBar) {
        BackHandler(enabled = needShowSuggests || needShowWordHistory || wordStack.size > 1) {
            if (needShowWordHistory) {
                vm.toggleWordHistory()
            } else if (needShowSuggests) {
                focusManager.clearFocus()
            } else if (wordStack.size > 1) {
                vm.onBackPressed()
            }
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
            CustomTopAppBar(
                actions = {
                    IconButton(
                        onClick = {
                            vm.toggleWordHistory()
                        }
                    ) {
                        Icon(
                            painter = painterResource(MR.images.word_history_menu),
                            contentDescription = null,
                            tint = if (needShowWordHistory) {
                                MaterialTheme.colors.secondary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                }
            ) {
                SearchView(
                    Modifier.weight(1.0f),
                    searchText.value,
                    focusRequester = focusRequester,
                    onTextChanged = {
                        vm.onWordTextUpdated(it)
                    },
                    onFocusChanged = {
                        needShowSuggests = it.isFocused
                        if (it.isFocused) {
                            vm.onSuggestsAppeared()
                        }
                    }
                ) {
                    vm.onWordSubmitted(searchText.value)
                    focusManager.clearFocus()
                }
            }
        }

        contentHeader()

        val suggestsRes = suggests.value
        val suggestsData = suggestsRes.data()
        if (needShowWordHistory) {
            wordHistoryUI(vm)
        } else if (needShowSuggests && suggestsData?.isNotEmpty() == true) {
            suggestListUI(suggestsData, vm, focusManager)
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
                    tryAgainText = stringResource(MR.strings.error_try_again),
                    emptyText = stringResource(MR.strings.error_no_definitions)
                ) {
                    vm.onTryAgainClicked()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun wordHistoryUI(
    vm: DefinitionsVM,
) {
    val words by vm.wordHistory.collectAsState()
    if (words.isLoaded()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                bottom = 300.dp
            )
        ) {
            items(words.data().orEmpty(), key = { it.id }) { item ->
                showWordHistoryItem(
                    Modifier.animateItemPlacement(),
                    item,
                    vm,
                )
            }
        }
    } else {
        LoadingStatusView(
            resource = words,
            loadingText = null,
            errorText = vm.getErrorText(words)?.localized(),
            tryAgainText = stringResource(MR.strings.error_try_again),
            emptyText = stringResource(MR.strings.error_empty_wordhistory)
        ) {
            vm.onTryAgainClicked()
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun suggestListUI(
    suggestsData: List<BaseViewItem<*>>,
    vm: DefinitionsVM,
    focusManager: FocusManager
) {
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
                    focusManager.clearFocus()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showWordHistoryItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DefinitionsVM,
) = when (item) {
    is WordHistoryViewItem -> {
        ListItem (
            modifier = modifier
                .clickable {
                    vm.onWordHistoryClicked(item)
                },
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showSuggestItem(
    modifier: Modifier,
    item: BaseViewItem<*>,
    vm: DefinitionsVM,
    onClicked: () -> Unit
) = when (item) {
    is WordSuggestDictEntryViewItem -> {
        ListItem (
            modifier = modifier
                .clickable {
                    vm.onWordSubmitted(item.firstItem())
                    onClicked.invoke()
                },
            secondaryText = { Text(item.source) },
            text = { Text(item.firstItem()) }
        )
    }
    is WordTextSearchHeaderViewItem -> ListSectionCell(
        item.firstItem().localized(),
        Modifier.padding(
            top = if (item.isTop) 0.dp else 16.dp
        ),
        actionBlock = {
            Text(
                text = item.showAllWordsActionText.localized(),
                modifier = Modifier.clickable {
                        vm.onSuggestedShowAllSearchWordClicked()
                        onClicked.invoke()
                    }.padding(
                        start = LocalDimens.current.contentPadding,
                        end = LocalDimens.current.contentPadding
                    ),
                color = MaterialTheme.colors.secondary,
                style = LocalAppTypography.current.wordDefinitionSubHeader
            )
        }
    )
    is WordSuggestByTextViewItem -> {
        ListItem (
            modifier = modifier
                .clickable {
                    vm.onSuggestedSearchWordClicked(item)
                    onClicked.invoke()
                },
            secondaryText = { Text(item.source) },
            text = { Text(item.firstItem()) }
        )
    }
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
            TextWithWordClickHandler(text, ts) {
                vm.onWordSubmitted(it)
            }
            if (item.withAddButton) {
                AddToSet(vm, item)
            }
        }
    )
    is WordSubHeaderViewItem -> WordSubHeaderView(item, modifier)
    is WordSynonymViewItem -> WordSynonymView(
        item,
        modifier,
        textContent = { text, ts ->
            TextWithWordClickHandler(text, ts) {
                vm.onWordSubmitted(it)
            }
        }
    )
    is WordExampleViewItem -> WordExampleView(
        item,
        modifier,
        textContent = { text, ts ->
            TextWithWordClickHandler(text, ts) {
                vm.onWordSubmitted(it)
            }
        },
    )
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
                        is CardSetExpandOrCollapseViewItem -> DropdownMenuItem(
                            onClick = {
                                vm.onCardSetExpandCollapseClicked(it)
                            }
                        ) {
                            Text(
                                it.text.localized(),
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        is OpenCardSetViewItem -> DropdownMenuItem(
                            onClick = {
                                vm.onOpenCardSets(it)
                                expanded = false
                            }
                        ) {
                            Text(
                                it.text.localized(),
                                color = MaterialTheme.colors.secondary
                            )
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
                modifier = Modifier.width(IntrinsicSize.Min),
                textAlign = TextAlign.End,
                style = LocalAppTypography.current.wordDefinitionProvidedBy
            )
        }

        if (viewItem.frequencyLevelAndRatio != null &&
            viewItem.frequencyLevelAndRatio.level != WordFrequencyGradation.UNKNOWN_LEVEL) {
            Box(
                Modifier
                    .padding(
                        start = 4.dp,
                        top = 10.dp,
                        end = 2.dp,
                        bottom = 10.dp,
                    )
                    .size(20.dp)
                    .run {
                        if (viewItem.frequencyLevelAndRatio.level != WordFrequencyGradation.UNKNOWN_LEVEL) {
                            background(
                                color = wordFrequencyColor(viewItem.frequencyLevelAndRatio.ratio),
                                shape = RoundedCornerShape(20.dp)
                            )
                        } else {
                            this
                        }
                    }
                    .border(
                        1.dp,
                        color = MaterialTheme.colors.secondary,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = (viewItem.frequencyLevelAndRatio.level + 1).toString(),
                    modifier = Modifier.align(Alignment.Center).offset(y = (-1).dp),
                    textAlign = TextAlign.Center,
                    style = LocalAppTypography.current.wordFrequency
                )
            }
        }
    }
}

@Composable
fun wordFrequencyColor(ratio: Float?): Color {
    if (ratio == null) {
        return Color.Transparent
    }

    return MaterialTheme.colors.secondary.copy(alpha = 1 - ratio)
//    return lerp(
//        StartWordFrequencyColor,
//        EndWordFrequencyColor,
//        level,
//    )
}

//private val StartWordFrequencyColor = MaterialTheme.colors.secondary
//private val EndWordFrequencyColor = Color(0xFFFF634D)

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

@OptIn(ExperimentalLayoutApi::class)
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
    },
    labelContent: @Composable RowScope.(text: String, index: Int) -> Unit = { text, _ ->
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    },
    lastLabel: (@Composable FlowRowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(
            start = LocalDimensWord.current.wordHorizontalPadding,
            end = LocalDimensWord.current.wordHorizontalPadding,
            top = LocalDimensWord.current.wordHeaderTopMargin
        )
    ) {
        if (viewItem.labels.isNotEmpty() || lastLabel != null) {
            WordLabels(
                viewItem.labels,
                modifier = Modifier.padding(start = 10.dp, end = 24.dp),
                labelContent,
                lastLabel,
            )
        }
        Row {
            Text(" • ")
            textContent(viewItem.firstItem(), textStyle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordLabels(
    labels: List<String>,
    modifier: Modifier = Modifier,
    textContent: @Composable RowScope.(text: String, index: Int) -> Unit,
    lastItem: (@Composable FlowRowScope.() -> Unit)? = null
) {
    FlowRow(modifier = modifier) {
        labels.mapIndexed { index, value ->
            CustomBadge(
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 2.dp, bottom = 2.dp, end = 2.dp),
                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colors.onSecondary,
                content = {
                    textContent(value, index)
                }
            )
        }
        lastItem?.let { it() }
    }
}

@Composable
fun CustomBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp)
            )
            .clip(CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (content != null) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                val style = MaterialTheme.typography.button.copy(fontSize = 10.sp)
                ProvideTextStyle(
                    value = style,
                    content = { content() }
                )
            }
        }
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

@Composable
private fun RowScope.TextWithWordClickHandler(
    text: String,
    textStyle: TextStyle,
    onWordClicked: (String) -> Unit
) {
    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    Text(
        modifier = Modifier.weight(1.0f)
            .onWordClick(text, { textLayoutResult }) { word ->
                onWordClicked(word)
            },
        text = text,
        style = textStyle,
        onTextLayout = {
            textLayoutResult = it
        }
    )
}

private fun Modifier.onWordClick(
    text: String,
    textLayoutResult: () -> TextLayoutResult?,
    onWordClicked: (String) -> Unit
): Modifier = this then pointerInput("touchDetect") {
    detectTapGestures { pos ->
        textLayoutResult.invoke()?.let { layoutResult ->
            val offset = layoutResult.getOffsetForPosition(pos)
            findWordInString(text, offset)?.let { word ->
                onWordClicked(word)
            }
        }
    }
}

private fun findWordInString(str: String, index: Int): String? {
    if (index < 0 || index >= str.length) {
        return null
    }

    var i = index
    // take word on the left if index points at ' '
    while (str[i] == ' ' && i > 0) {
        i -= 1
    }

    if (i < 0) {
        return null
    }

    var startI = i
    var endI = i
    if (str.getOrNull(startI)?.isLetter() == true) {
        while (startI > 0 && str[startI - 1].isLetter()) {
            startI -= 1
        }
    }

    if (str.getOrNull(endI)?.isLetter() == true) {
        while (endI < str.length && str[endI].isLetter()) {
            endI += 1
        }
    }

    if (startI != endI && startI != endI - 1) {
        return str.substring(startI, endI)
    }

    return null
}