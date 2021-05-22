package com.aglushkov.wordteacher.androidApp.features.definitions.views

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.toDp
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.Chip
import com.aglushkov.wordteacher.androidApp.general.views.compose.ChipColors
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDividerViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTranscriptionViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.repository.config.Config
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import java.io.IOException
import java.util.*

@Composable
fun DefinitionsUI(vm: DefinitionsVM) {
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
                    showViewItem(item, vm)
                }
            }
        } else {
            LoadingStatusView(
                resource = res,
                loadingText = null,
                errorText = vm.getErrorText(res)?.resolveString(),
                emptyText = LocalContext.current.getString(R.string.error_no_definitions)
            ) {
                vm.onTryAgainClicked()
            }
        }
    }
}

@Composable
private fun showViewItem(
    item: BaseViewItem<*>,
    vm: DefinitionsVM
) = when (item) {
    is DefinitionsDisplayModeViewItem -> DefinitionsDisplayModeView(
        item,
        { vm.onPartOfSpeechFilterClicked(item) },
        { vm.onPartOfSpeechFilterCloseClicked(item) },
        { mode -> vm.onDisplayModeChanged(mode) }
    )
    is WordDividerViewItem -> WordDividerView()
    is WordTitleViewItem -> WordTitleView(item)
    is WordTranscriptionViewItem -> WordTranscriptionView(item)
    is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item)
    is WordDefinitionViewItem -> WordDefinitionView(item)
    is WordSubHeaderViewItem -> WordSubHeaderView(item)
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
    val horizontalPadding = dimensionResource(R.dimen.definitions_displayMode_horizontal_padding)
    val topPadding = dimensionResource(R.dimen.definitions_displayMode_vertical_padding)
    Row(
        modifier = Modifier
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
private fun WordDividerView(
) {
    Divider(
        modifier = Modifier.padding(
            top = dimensionResource(id = R.dimen.word_divider_topMargin),
            bottom = dimensionResource(id = R.dimen.word_divider_bottomMargin)
        )
    )
}

@Composable
private fun WordTitleView(
    viewItem: WordTitleViewItem
) {
    val providedByString = stringResource(R.string.word_providedBy_template, viewItem.providers.joinToString())
    Row(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.word_horizontalPadding),
            end = dimensionResource(id = R.dimen.word_horizontalPadding)
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
                .widthIn(max = dimensionResource(id = R.dimen.word_providedBy_maxWidth)),
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
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding)
            ),
        style = AppTypography.wordDefinitionTranscripton
    )
}

@Composable
fun WordPartOfSpeechView(viewItem: WordPartOfSpeechViewItem) {
    Text(
        text = viewItem.firstItem().resolveString().toUpperCase(Locale.getDefault()),
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = dimensionResource(id = R.dimen.word_partOfSpeech_topMargin)
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
                start = dimensionResource(id = R.dimen.word_horizontalPadding),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = dimensionResource(id = R.dimen.word_header_topMargin)
            ),
        style = AppTypography.wordDefinition
    )
}

@Composable
fun WordSubHeaderView(viewItem: WordSubHeaderViewItem) {
    Text(
        text = viewItem.firstItem().resolveString(),
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.word_horizontalPadding) + Dp(viewItem.indent.toDp(
                    LocalContext.current.resources) / LocalDensity.current.density),
                end = dimensionResource(id = R.dimen.word_horizontalPadding),
                top = dimensionResource(id = R.dimen.word_subHeader_topMargin)
            ),
        style = AppTypography.wordDefinitionSubHeader
    )
}

// Previews

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
                        WordPartOfSpeechViewItem(StringDesc.Raw("Noun")),
                        WordDefinitionViewItem("* definition 1"),
                        WordDefinitionViewItem("* definition 2"),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 1"), Indent.NONE),
                        WordSubHeaderViewItem(StringDesc.Raw("Subheader 2"), Indent.SMALL),
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun DefinitionsUIPreviewLoading() {
    ComposeAppTheme {
        DefinitionsUI(
            DefinitionsVMPreview(Resource.Loading())
        )
    }
}

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
