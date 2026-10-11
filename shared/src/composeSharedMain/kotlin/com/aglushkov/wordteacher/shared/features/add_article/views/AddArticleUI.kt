package com.aglushkov.wordteacher.shared.features.add_article.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wordteacher.shared.generated.resources.Res
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.resource.RESOURCE_UNDEFINED_PROGRESS
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resources.resolve
import com.aglushkov.wordteacher.shared.general.settings.HintType
import com.aglushkov.wordteacher.shared.general.views.HintView
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.OutlinedTextFieldWithError
import com.aglushkov.wordteacher.shared.general.views.windowInsetsHorizontalPadding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import wordteacher.shared.generated.resources.add_article_create_set_option
import wordteacher.shared.generated.resources.add_article_done
import wordteacher.shared.generated.resources.add_article_field_text_hint
import wordteacher.shared.generated.resources.add_article_field_title_hint
import wordteacher.shared.generated.resources.add_article_title
import wordteacher.shared.generated.resources.close_24

@Composable
fun AddArticleUIDialog(
    vm: AddArticleVM,
    modifier: Modifier = Modifier,
) {
    CustomDialogUI(
        onDismissRequest = { vm.onClosed() }
    ) {
        AddArticleUI(
            vm = vm,
            modifier = modifier,
            actions = {
                IconButton(
                    onClick = { vm.onCancelPressed() }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            },
        )
    }
}

@Composable
fun AddArticleUI(
    vm: AddArticleVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val uiState by vm.uiStateFlow.collectAsState()
    val needShowFloatingActionButton by remember(uiState) { derivedStateOf { uiState.isLoaded() } }
    val addingState by vm.addingStateFlow.collectAsState()
    val isAdding by remember(addingState){ derivedStateOf { addingState.isLoading() } }
    val addingProgress by remember(addingState){ derivedStateOf { addingState.progress() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Column {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.add_article_title)) },
                actions = actions
            )

            AddArticlesFieldsUI(
                vm = vm,
                uiState = uiState
            )
        }

        if (needShowFloatingActionButton) {
            ExtendedFloatingActionButton(
                text = {
                    if (isAdding) {
                        if (addingProgress == RESOURCE_UNDEFINED_PROGRESS) {
                            CircularProgressIndicator(
                                modifier = Modifier.then(Modifier.size(30.dp)),
                                color = contentColorFor(MaterialTheme.colors.secondary)
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = addingProgress,
                                modifier = Modifier.then(Modifier.size(30.dp)),
                                color = contentColorFor(MaterialTheme.colors.secondary)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.add_article_done),
                            modifier = Modifier.padding(horizontal = LocalDimens.current.contentPadding)
                        )
                    }
                },
                onClick = { vm.onCompletePressed() },
                modifier = Modifier.Companion
                    .align(Alignment.BottomEnd)
                    .padding(LocalDimens.current.contentPadding)
                    .windowInsetsHorizontalPadding()
            )
        }
    }
}

@Composable
private fun AddArticlesFieldsUI(
    vm: AddArticleVM,
    uiState: Resource<AddArticleVM.UIState>
) {
    val data = uiState.data()

    if (!uiState.isLoaded() || data == null) {
        LoadingStatusView(
            resource = uiState,
            loadingText = null,
            errorText = vm.getErrorText()?.resolve(),
        ) {
            vm.onTryAgainPressed()
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        val scrollableState = rememberScrollState()
        var wasTitleFocused by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        val titleState = rememberTextFieldState(data.title)
        val textState = rememberTextFieldState(data.text)

        LaunchedEffect(titleState) {
            snapshotFlow { titleState.text }
                .drop(1)
                .collectLatest { text ->
                    vm.onTitleChanged(text.toString())
                }
        }
        LaunchedEffect(textState) {
            snapshotFlow { textState.text }
                .drop(1)
                .collectLatest { text ->
                    vm.onTextChanged(text.toString())
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsHorizontalPadding()
                .verticalScroll(scrollableState)
                .padding(
                    top = LocalDimens.current.contentPadding,
                    bottom = 88.dp,
                )
        ) {
            val visibleHintType = data.visibleHintType
            if (visibleHintType != null) {
                HintView(
                    hintType = visibleHintType,
                    contentPadding = PaddingValues(LocalDimens.current.contentPadding),
                    onHidden = { vm.onHintClicked(visibleHintType) }
                )
            }

            OutlinedTextFieldWithError(
                state = titleState,
                modifier = Modifier.padding(
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding,
                ),
                hint = stringResource(Res.string.add_article_field_title_hint),
                errorText = data.titleError,
                onFocusChanged = {
                    if (it.isFocused) {
                        wasTitleFocused = true
                    }

                    if (wasTitleFocused) {
                        vm.onTitleFocusChanged(it.isFocused)
                    }
                },
                focusRequester = focusRequester,
                focusManager = focusManager,
            )

            if (data.showNeedToCreateCardSet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { vm.onNeedToCreateSetPressed() })
                        .padding(
                            top = 16.dp,
                            bottom = 16.dp,
                            start = LocalDimens.current.contentPadding,
                            end = LocalDimens.current.contentPadding
                        )
                ) {
                    Text(
                        text = stringResource(Res.string.add_article_create_set_option),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .weight(1.0f)
                    )
                    Checkbox(
                        checked = data.needToCreateSet,
                        onCheckedChange = null
                    )
                }
            }

            OutlinedTextField(
                state = textState,
                modifier = Modifier
                    .padding(
                        start = LocalDimens.current.contentPadding,
                        end = LocalDimens.current.contentPadding,
                    )
                    .fillMaxWidth()
                    .sizeIn(
                        minHeight = with(LocalDensity.current) {
                            (42 * 2).sp.toDp()
                        },
                        maxHeight = 600.dp
                    ),
                label = {
                    Text(stringResource(Res.string.add_article_field_text_hint))
                }
            )
        }

        LaunchedEffect("focus") {
            focusRequester.requestFocus()
        }
    }
}
