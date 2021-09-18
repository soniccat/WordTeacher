package com.aglushkov.wordteacher.desktopapp.features.definitions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import com.aglushkov.wordteacher.desktopapp.compose.AppTypography
import com.aglushkov.wordteacher.desktopapp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.desktopapp.general.views.compose.Chip
import com.aglushkov.wordteacher.desktopapp.general.views.compose.ChipColors
import com.aglushkov.wordteacher.desktopapp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.desktopapp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.desktopapp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.events.EmptyEvent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.ShowPartsOfSpeechFilterDialogEvent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTranscriptionViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DefinitionsUI(vm: DefinitionsVM) {
    val scope = rememberCoroutineScope()
    val partsOfSpeech by vm.partsOfSpeechFilterStateFlow.collectAsState()
    val selectedPartsOfSpeeches by vm.selectedPartsOfSpeechStateFlow.collectAsState()
    val event = vm.eventFlow.collectAsState(initial = EmptyEvent)
    val partOfSpeechFilterBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

//    ChooserUI(
//        state = partOfSpeechFilterBottomSheetState,
//        items = partsOfSpeech.map { partOfSpeech ->
//            val isSelected = selectedPartsOfSpeeches.contains(partOfSpeech)
//            ChooserViewItem(0, partOfSpeech.name, partOfSpeech, isSelected)
//        },
//        onSelected = { items ->
//            vm.onPartOfSpeechFilterUpdated(
//                items.filter { option ->
//                    option.isSelected
//                }.map { option ->
//                    option.obj as WordTeacherWord.PartOfSpeech
//                }
//            )
//        }
//    ) {
        DefinitionsWordUI(
            vm,
            onPartOfSpeechFilterClicked = { items ->
                scope.launch {
                    partOfSpeechFilterBottomSheetState.show()
                }
            }
        )
//    }
}

@Composable
private fun DefinitionsWordUI(
    vm: DefinitionsVM,
    onPartOfSpeechFilterClicked: (item: DefinitionsDisplayModeViewItem) -> Unit
) {
    val defs = vm.definitions.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CustomTopAppBar {
                SearchView(searchText, { searchText = it }) {
                    vm.onWordSubmitted(searchText)
                }
            }
        },
        bottomBar = {
        }
    ) {
        val res = defs.value
        val data = res.data()

        if (data?.isNotEmpty() == true) {
            LazyColumn {
                items(data) { item ->
                    showViewItem(item, vm, onPartOfSpeechFilterClicked)
                }
            }
        } else {
            LoadingStatusView(
                resource = res,
                loadingText = null,
                errorText = vm.getErrorText(res)?.toResultString(),
                emptyText = "R.string.error_no_definitions"
            ) {
                vm.onTryAgainClicked()
            }
        }
    }
}

@Composable
private fun showViewItem(
    item: BaseViewItem<*>,
    vm: DefinitionsVM,
    onPartOfSpeechFilterClicked: (item: DefinitionsDisplayModeViewItem) -> Unit
) = when (item) {
    is DefinitionsDisplayModeViewItem -> DefinitionsDisplayModeView(
        item,
        { onPartOfSpeechFilterClicked(item) },
        { vm.onPartOfSpeechFilterCloseClicked(item) },
        { mode -> vm.onDisplayModeChanged(mode) }
    )
    is WordDividerViewItem -> WordDividerView()
    is WordTitleViewItem -> WordTitleView(item)
    is WordTranscriptionViewItem -> WordTranscriptionView(item)
    is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item)
    is WordDefinitionViewItem -> WordDefinitionView(item)
    is WordSubHeaderViewItem -> WordSubHeaderView(item)
    is WordSynonymViewItem -> WordSynonymView(item)
    is WordExampleViewItem -> WordExampleView(item)
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

@Composable
private fun DefinitionsDisplayModeView(
    item: DefinitionsDisplayModeViewItem,
    onPartOfSpeechFilterClicked: () -> Unit,
    onPartOfSpeechFilterCloseClicked: () -> Unit,
    onDisplayModeChanged: (mode: DefinitionsDisplayMode) -> Unit
) {
    val horizontalPadding = 15.0f
    val topPadding = 15.0f
    Row(
        modifier = Modifier
            .padding(
                start = Dp(horizontalPadding),
                end = Dp(horizontalPadding),
                top = Dp(topPadding)
            )
            .horizontalScroll(rememberScrollState())
    ) {
        Chip(
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 4.dp
            ),
            text = item.partsOfSpeechFilterText.toResultString(),
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

        Spacer(modifier = Modifier.width(Dp(15.0f)))

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
                text = mode.toStringDesc().toResultString(),
                isChecked = mode == selectedMode
            ) {
                onDisplayModeChanged(mode)
            }
        }
    }
}

@Composable
private fun WordDividerView(
) {
    Divider(
        modifier = Modifier.padding(
            top = Dp(15.0f),
            bottom = Dp(15.0f)
        )
    )
}

@Composable
private fun WordTitleView(
    viewItem: WordTitleViewItem
) {
    val providedByString = "provided by"
    Row(
        modifier = Modifier.padding(
            start = Dp(15.0f),
            end = Dp(15.0f)
        )
    ) {
        Text(
            text = viewItem.firstItem(),
            modifier = Modifier
                .weight(1.0f, true),
            style = AppTypography.wordDefinitionTitle
        )
        Text(
            text = providedByString,
            modifier = Modifier
                .widthIn(max = Dp(15.0f)),
            textAlign = TextAlign.End,
            style = AppTypography.wordDefinitionProvidedBy
        )
    }
}

@Composable
fun WordTranscriptionView(viewItem: WordTranscriptionViewItem) {
    Text(
        text = viewItem.firstItem(),
        modifier = Modifier
            .padding(
                start = Dp(15.0f),
                end = Dp(15.0f)
            ),
        style = AppTypography.wordDefinitionTranscripton
    )
}

@Composable
fun WordPartOfSpeechView(viewItem: WordPartOfSpeechViewItem) {
    Text(
        text = viewItem.firstItem().toResultString().toUpperCase(Locale.getDefault()),
        modifier = Modifier
            .padding(
                start = Dp(15.0f),
                end = Dp(15.0f),
                top = Dp(15.0f)
            ),
        style = AppTypography.wordDefinitionPartOfSpeech
    )
}

@Composable
fun WordDefinitionView(viewItem: WordDefinitionViewItem) {
    Text(
        text = viewItem.firstItem(),
        modifier = Modifier
            .padding(
                start = Dp(15.0f),
                end = Dp(15.0f),
                top = Dp(15.0f)
            ),
        style = AppTypography.wordDefinition
    )
}

@Composable
fun WordSubHeaderView(viewItem: WordSubHeaderViewItem) {
    Text(
        text = viewItem.firstItem().toResultString(),
        modifier = Modifier
            .padding(
                start = Dp(15.0f) + viewItem.indent.toDp(),
                end = Dp(15.0f),
                top = Dp(15.0f)
            ),
        style = AppTypography.wordDefinitionSubHeader
    )
}

@Composable
fun WordSynonymView(viewItem: WordSynonymViewItem) {
    Text(
        text = viewItem.firstItem(),
        modifier = Modifier
            .padding(
                start = Dp(15.0f) + viewItem.indent.toDp(),
                end = Dp(15.0f)
            ),
        style = AppTypography.wordSynonym
    )
}

@Composable
fun WordExampleView(viewItem: WordExampleViewItem) {
    Text(
        text = viewItem.firstItem(),
        modifier = Modifier
            .padding(
                start = Dp(15.0f) + viewItem.indent.toDp(),
                end = Dp(15.0f)
            ),
        style = AppTypography.wordExample
    )
}

// Previews

//@Preview
//@Composable
//private fun DefinitionsUIPreviewWithResponse() {
//    ComposeAppTheme {
//        DefinitionsUI(
//            DefinitionsVMPreview(
//                Resource.Loaded(
//                    listOf(
//                        DefinitionsDisplayModeViewItem(
//                            partsOfSpeechFilterText = StringDesc.Raw("Noun"),
//                            canClearPartsOfSpeechFilter = true,
//                            modes = listOf(DefinitionsDisplayMode.BySource, DefinitionsDisplayMode.Merged),
//                            selectedIndex = 0
//                        ),
//                        WordDividerViewItem(),
//                        WordTitleViewItem(
//                            title = "Word",
//                            providers = listOf(Config.Type.Yandex)
//                        ),
//                        WordTitleViewItem(
//                            title = "Word 2",
//                            providers = listOf(Config.Type.Yandex, Config.Type.Google, Config.Type.OwlBot)
//                        ),
//                        WordTranscriptionViewItem("[omg]"),
//                        WordPartOfSpeechViewItem(StringDesc.Raw("Noun")),
//                        WordDefinitionViewItem("* definition 1"),
//                        WordDefinitionViewItem("* definition 2"),
//                        WordSynonymViewItem("synonym 1", Indent.NONE),
//                        WordSynonymViewItem("synonym 2", Indent.SMALL),
//                        WordExampleViewItem("example 1", Indent.NONE),
//                        WordExampleViewItem("example 2", Indent.SMALL),
//                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 1"), Indent.NONE),
//                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 2"), Indent.SMALL),
//                    )
//                )
//            )
//        )
//    }
//}

//@Preview
//@Composable
//private fun DefinitionsUIPreviewLoading() {
//    ComposeAppTheme {
//        DefinitionsUI(
//            DefinitionsVMPreview(Resource.Loading())
//        )
//    }
//}

//@Preview
//@Composable
//private fun DefinitionsUIPreviewError() {
//    ComposeAppTheme {
//        DefinitionsUI(
//            DefinitionsVMPreview(
//                Resource.Error(
//                    IOException("Sth went wrong"),
//                    true
//                )
//            )
//        )
//    }
//}

@Composable
fun Indent.toDp(): Dp {
    val px = when (this) {
        Indent.SMALL -> 15.0f
        Indent.NONE -> 0.0f
    }
    return Dp(px)
}