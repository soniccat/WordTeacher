package com.aglushkov.wordteacher.shared.features.cardsets.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CreateCardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.RemoteCardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.SectionViewItem
import com.aglushkov.wordteacher.shared.general.BackHandler
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndNotEmpty
import com.aglushkov.wordteacher.shared.general.views.*
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
    var searchText by remember { mutableStateOf(vm.uiStateFlow.value.searchQuery.orEmpty()) }

    val needShowSearchResult by remember(searchCardSets) { derivedStateOf { !searchCardSets.isUninitialized() } }
    val uiState = vm.uiStateFlow.collectAsState()
    val newCardSetState by remember { mutableStateOf(TextFieldCellStateImpl { uiState.value.newCardSetText }) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = needShowSearchResult) {
        coroutineScope.launch {
            searchText = ""
            vm.onSearchClosed()
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
                    searchText,
                    onTextChanged = {
                        searchText = it
                        if (it.isEmpty()) {
                            vm.onSearchClosed()
                        }
                    }
                ) {
                    if (searchText.isEmpty()) {
                        //vm.onSearchClosed()
                        focusManager.clearFocus()
                    } else {
                        vm.onSearch(searchText)
                    }
                }
                if (vm.availableFeatures.canImportCardSetFromJson) {
                    AddIcon {
                        vm.onJsonImportClicked()
                    }
                }
            }

            // search result
            if (needShowSearchResult) {
                val data = searchCardSets.data()
                if (searchCardSets.isLoadedAndNotEmpty() && data != null) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(
                            data,
                            key = { it.id }
                        ) { item ->
                            ShowSearchCardSets(item, vm)
                        }
                    }
                } else {
                    LoadingStatusView(
                        resource = searchCardSets,
                        loadingText = null,
                        errorText = vm.getErrorText().localized(),
                        emptyText = vm.getEmptySearchText().localized(),
                    ) {
                        vm.onTryAgainSearchClicked()
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

        Box(
            modifier = Modifier.matchParentSize(),
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
//        Text(
//        item.name.localized(),
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(
//                top = if (item.isTop) 0.dp else 16.dp
//            )
//            .background(color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f))
//            .padding(
//                start = LocalDimens.current.contentPadding,
//                end = LocalDimens.current.contentPadding
//            ),
//        style = LocalAppTypography.current.wordDefinitionSubHeader
//    )
    is CardSetViewItem -> CardSetItemView(
        item,
        onClick = { vm.onCardSetClicked(item) },
        onDeleted = { vm.onCardSetRemoved(item) }
    )
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CardSetItemView(
    item: CardSetViewItem,
    onClick: () -> Unit = {},
    onDeleted: () -> Unit = {}
) {
    DeletableCell(
        Modifier,
        stateKey = item.id,
        enabled = true,
        onClick,
        onDeleted
    ) {
        CardSetItemView(
            Modifier,
            item
        )
    }
}

@Composable
fun CardSetItemView(
    modifier: Modifier = Modifier,
    item: CardSetViewItem,
) {
//        Box(
//            modifier = Modifier.fillMaxWidth()
//        ) {
    CustomTextListItem(
        modifier = modifier,
        title = item.name,
        subtitle = item.terms.joinToString(),
        trailing = {
            val side = 30.dp
            Box(
                modifier = Modifier.size(side, side)
            ) {
                CircularProgressIndicator(
                    progress = 1.0f,
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
                CircularProgressIndicator(progress = item.totalProgress)
            }
        }
    )
//            LinearProgressIndicator(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomStart),
//                progress = item.readyToLearnProgress,
//                color = MaterialTheme.colors.secondary
//            )
//        }
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
        subtitle = item.terms.joinToString().takeIf { it.isNotEmpty() },
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
