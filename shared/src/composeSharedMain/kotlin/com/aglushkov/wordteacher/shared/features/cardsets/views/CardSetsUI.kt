package com.aglushkov.wordteacher.shared.features.cardsets.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CreateCardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.SectionViewItem
import com.aglushkov.wordteacher.shared.features.dashboard.vm.HintViewItem
import com.aglushkov.wordteacher.shared.general.BackHandler
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.replaceFirstToCapital
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndNotEmpty
import com.aglushkov.wordteacher.shared.general.resource.onData
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.general.views.*
import com.aglushkov.wordteacher.shared.model.CardSetTag
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.launch
import dev.icerock.moko.resources.compose.localized

@Composable
fun CardSetsUI(
    vm: CardSetsVM,
    modifier: Modifier = Modifier,
    onBackHandler: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val cardSets by vm.cardSets.collectAsState()
    val searchCardSets by vm.searchCardSets.collectAsState()
    val searchTags by vm.searchTags.collectAsState()

    val uiState by vm.uiStateFlow.collectAsState()
    val newCardSetState by remember { mutableStateOf(TextFieldCellStateImpl { uiState.newCardSetText }) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val searchSelectionKey = remember { mutableIntStateOf(0) }

    BackHandler(enabled = uiState.needShowSearch) {
        coroutineScope.launch {
            vm.onSearchClosed()
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        Column{
            CustomTopAppBar {
                onBackHandler?.let { onBackHandler ->
                    IconButton(
                        onClick = { onBackHandler() }
                    ) {
                        Icon(
                            painter = painterResource(MR.images.arrow_back_24),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                }

                SearchView(
                    modifier = Modifier.weight(1.0f),
                    uiState.searchQuery.orEmpty(),
                    selectionKey = searchSelectionKey.value,
                    focusRequester = run {
                        val event = uiState.focusEvent
                        if (event is CardSetsVM.FocusEvent && event.type == CardSetsVM.ElementType.Search) {
                            focusRequester
                        } else {
                            null
                        }
                    },
                    onTextChanged = {
                        vm.onSearchTextChanged(it)
                        if (it.isEmpty()) {
                            vm.onSearchClosed()
                        }
                    },
                    onFocusChanged = {
                        vm.onSearchFocusChanged(it.isFocused)
                    }
                ) {
                    if (uiState.searchQuery?.isEmpty() == true) {
                        //vm.onSearchClosed()
                        focusManager.clearFocus()
                    } else {
                        vm.onSearch(uiState.searchQuery.orEmpty())
                    }
                }
                if (vm.availableFeatures.canImportCardSetFromJson) {
                    AddIcon {
                        vm.onJsonImportClicked()
                    }
                }
            }

            // search result
            if (uiState.needShowSearch) {
                val data = searchCardSets.data()
                if (searchCardSets.isLoadedAndNotEmpty() && data != null) {
                    LazyColumn(
                        modifier = Modifier.windowInsetsHorizontalPadding(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(
                            data,
                            key = { it.id }
                        ) { item ->
                            ShowSearchCardSets(item, vm)
                        }
                    }
                } else if (!searchCardSets.isUninitialized()) {
                    LoadingStatusView(
                        resource = searchCardSets,
                        loadingText = null,
                        errorText = vm.getErrorText().localized(),
                        emptyText = vm.getEmptySearchText().localized(),
                    ) {
                        vm.onTryAgainSearchClicked()
                    }
                // show tags
                } else if (uiState.needShowCardSetTags) {
                    searchTags.onData {
                        ShowCardSetTags(it, vm, searchSelectionKey)
                    }
                }
            } else {
                // local cardsets
                val data = cardSets.data()
                if (cardSets.isLoaded() && data != null) {
//                    if (data.size == 1 && LocalIsDebug.current) {
//                        importDBButton()
//                    }

                    LazyColumn(
                        modifier = Modifier.windowInsetsHorizontalPadding(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(
                            data,
                            key = { it.id }
                        ) { item ->
                            CardSetsViewItem(item, vm, newCardSetState)
                        }
                    }
                } else {
                    LoadingStatusView(
                        resource = cardSets,
                        loadingText = null,
                        errorText = vm.getErrorText().localized(),
                    ) {
                        vm.onTryAgainClicked()
                    }
                }
            }
        }

        if (!uiState.needShowSearch) {
            Box(
                modifier = Modifier.matchParentSize().windowInsetsHorizontalPadding(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { vm.onStartLearningClicked() },
                    modifier = Modifier.padding(LocalDimensWord.current.articleHorizontalPadding)
                ) {
                    Icon(
                        painter = painterResource(MR.images.start_learning_24),
                        contentDescription = null
                    )
                }
            }
        }
    }

    if (uiState.focusEvent != null) {
        LaunchedEffect("focusEvent") {
            focusRequester.requestFocus()
            vm.onFocusEventHandled()
        }
    }
}

@Composable
private fun ShowSearchCardSets(
    item: BaseViewItem<*>,
    vm: CardSetsVM
) {
    when (item) {
        is RemoteCardSetViewItem -> {
            CardSetSearchItemView(
                item,
                onClick = { vm.onSearchCardSetClicked(item) },
                onAdded = { vm.onSearchCardSetAddClicked(item) }
            )
        }
        else -> {
            Text(
                text = "unknown item $item"
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ShowCardSetTags(
    tags: List<CardSetTag>,
    vm: CardSetsVM,
    searchSelectionKey: MutableIntState
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .fillMaxHeight()
            .windowInsetsHorizontalPadding()
            .verticalScroll(rememberScrollState())
    ) {
        FlowRow(
            Modifier.padding(LocalDimens.current.contentPadding)
        ) {
            tags.onEach {
                Chip(
                    onClick = {
                        searchSelectionKey.value += 1
                        vm.onCardSetTagClicked(it)
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = it.name.replaceFirstToCapital() + " • " + it.count
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSetsViewItem(
    item: BaseViewItem<*>,
    vm: CardSetsVM,
    newCardSetState: TextFieldCellState,
) = when (item) {
    is CreateCardSetViewItem -> TextFieldCellView(
        placeholder = item.placeholder.localized(),
        textFieldValue = newCardSetState.rememberTextFieldValueState(),
        focusRequester = newCardSetState.focusRequester,
        onTextChanged = {
            newCardSetState.updateTextFieldValue(it)  // update UI text field state
            vm.onNewCardSetTextChange(it.text) // update VM text state
        },
        onCreated = {
            vm.onCardSetAdded(it)
        }
    )
    is SectionViewItem -> ListSectionCell(
        item.name.localized(),
        Modifier.padding(
            top = if (item.isTop) 0.dp else 16.dp
        )
    )
    is CardSetViewItem -> CardSetWithTotalProgressItemView(
        item,
        onClick = { vm.onCardSetClicked(item) },
        onStartLearningClick = { vm.onCardSetStartLearningClicked(item) },
        onDeleted = { vm.onCardSetRemoved(item) }
    )
    is HintViewItem -> {
        HintView(
            hintType = item.firstItem(),
            contentPadding = PaddingValues(
                top = LocalDimens.current.contentPadding,
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
            ),
            onHidden = { vm.onHintClicked(item.firstItem()) }
        )
    }
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

@Composable
fun CardSetWithTotalProgressItemView(
    item: CardSetViewItem,
    onClick: () -> Unit = {},
    onStartLearningClick: () -> Unit = {},
    onDeleted: () -> Unit = {}
) {
    DeletableCell(
        Modifier,
        stateKey = item.id,
        enabled = true,
        onClick,
        onDeleted
    ) {
        CardSetWithTotalProgressItemView(
            Modifier,
            item,
            onStartLearningClick,
        )
    }
}

@Composable
fun CardSetWithTotalProgressItemView(
    modifier: Modifier = Modifier,
    item: CardSetViewItem,
    onStartLearningClick: () -> Unit = {},
) {
    CardSetItemView(
        modifier = modifier,
        item = item,
        trailing = {
            if (item.terms.isNotEmpty()) {
                val side = 40.dp
                Box(
                    modifier = Modifier.size(side, side)
                ) {
                    CircularProgressIndicator(
                        progress = 1.0f,
                        modifier = Modifier.padding(5.dp),
                        color = Color.LightGray.copy(alpha = 0.2f)
                    )
                    CircularProgressIndicator(
                        progress = item.totalProgress,
                        modifier = Modifier.padding(5.dp),
                    )
                    StartLearningButton(
                        modifier = Modifier.clickable {
                            onStartLearningClick()
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun CardSetItemView(
    modifier: Modifier = Modifier,
    item: CardSetViewItem,
    trailing: @Composable (CardSetViewItem) -> Unit
) {
    CustomTextListItem(
        modifier = modifier,
        title = item.name,
        subtitle = if (item.terms.isNotEmpty()) {
            "${item.terms.size} words: " + item.terms.joinToString()
        } else {
            ""
        },
        trailing = {
            trailing(item)
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CardSetSearchItemView(
    item: RemoteCardSetViewItem,
    onClick: () -> Unit = {},
    onAdded: (() -> Unit)? = null,
) {
    CustomTextListItem(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isRead) {0.5f} else {1.0f})
            .clickable {
                onClick()
            },
        title = item.name,
        subtitle = if (item.terms.isNotEmpty()) {
            "${item.terms.size} words: " + item.terms.joinToString()
        } else {
            ""
        },
        trailing = onAdded?.let { onAdded ->
            {
                if (item.isLoading) {
                    Box(modifier = Modifier.padding(LocalDimens.current.halfOfContentPadding).size(24.dp)) {
                        CircularProgressIndicator()
                    }
                } else {
                    AddIcon(style = AddIconStyle.Medium) {
                        onAdded()
                    }
                }
            }
        }
    )
}

//@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
//@Preview
//@Composable
//fun CardSetTitleViewPreviews() {
//    CardSetItemView(
//        CardSetViewItem(
//            setId = 0L,
//            name = "My card set",
//            date = "Today",
//            readyToLearnProgress = 0.3f,
//            totalProgress = 0.1f,
//        )
//    )
//}

//@ExperimentalAnimationApi
//@ExperimentalMaterialApi
//@ExperimentalComposeUiApi
//@Preview
//@Composable
//fun CardSetsUIPreviewWithArticles() {
//    ComposeAppTheme {
//        CardSetsUI(
//            vm = CardSetsVM(
//                articles = Resource.Loaded(
//                    data = listOf(
//                        ArticleViewItem(1, "Article Name", "Today")
//                    )
//                )
//            )
//        )
//    }
//}

//@Composable
//fun importDBButton() {
//    val context = LocalContext.current
//    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
//        result?.let {
//            val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
//            val importPath = context.getDatabasePath("test2").absolutePath.toPath()
//            context.contentResolver.openInputStream(result)?.buffered()?.use { stream ->
//                FileSystem.SYSTEM.write(importPath, true) {
//                    while (stream.read(byteArray) != -1) {
//                        write(byteArray)
//                    }
//                }
//            }
//
//            val dbPath = context.getDatabasePath("wt.db").absolutePath.toPath()
//            FileSystem.SYSTEM.delete(dbPath)
//            FileSystem.SYSTEM.atomicMove(importPath, dbPath)
//        }
//    }
//
//    return Column {
//        Button(onClick = {
//            launcher.launch("*/*")
//        }) {
//            Text("Import db file")
//        }
//    }
//}
