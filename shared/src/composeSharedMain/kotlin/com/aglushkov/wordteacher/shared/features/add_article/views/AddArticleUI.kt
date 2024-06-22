package com.aglushkov.wordteacher.shared.features.add_article.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.events.CompletionData
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoadedOrError
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.OutlinedTextFieldWithError
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

@Composable
fun AddArticleUIDialog(
    vm: AddArticleVM,
    modifier: Modifier = Modifier,
    onArticleCreated: () -> Unit
) {
    CustomDialogUI(
        onDismissRequest = { onArticleCreated() }
    ) {
        AddArticleUI(
            vm = vm,
            modifier = modifier,
            actions = {
                IconButton(
                    onClick = { vm.onCancelPressed() }
                ) {
                    Icon(
                        painter = painterResource(MR.images.close_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            },
            { onArticleCreated() }
        )
    }
}

@Composable
fun AddArticleUI(
    vm: AddArticleVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    onArticleCreated: SnackbarHostState.(articleId: Long?) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarStringDesc = remember { mutableStateOf<dev.icerock.moko.resources.desc.StringDesc?>(null) }
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
                title = { Text(text = stringResource(MR.strings.add_article_title)) },
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
                        CircularProgressIndicator(
                            progress = addingProgress,
                            modifier = Modifier.then(Modifier.size(30.dp)),
                            color = contentColorFor(MaterialTheme.colors.secondary)
                        )
                    } else {
                        Text(
                            text = stringResource(MR.strings.add_article_done),
                            modifier = Modifier.padding(horizontal = LocalDimens.current.contentPadding)
                        )
                    }
                },
                onClick = { vm.onCompletePressed() },
                modifier = Modifier.Companion
                    .align(Alignment.BottomEnd)
                    .padding(LocalDimens.current.contentPadding)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.Companion.align(Alignment.BottomCenter)
        ) {
            CustomSnackbar(
                message = snackbarStringDesc.value?.localized(),
                snackbarData = it,
            )
        }
    }

    LaunchedEffect("eventHandler") {
        vm.eventFlow.collect {
            when (it) {
                is CompletionEvent -> with(snackbarHostState) {
                // TODO: handle cancellation
                    onArticleCreated((it.data as? CompletionData.Article)?.id)
                }
                is ErrorEvent -> {
                    launch {
                        snackbarStringDesc.value = it.text
                        snackbarHostState.showSnackbar("")
                    }
                }
            }
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
            errorText = vm.getErrorText()?.localized(),
        ) {
            vm.onTryAgainPressed()
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        val scrollableState = rememberScrollState()
        var wasTitleFocused by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        var titleState by remember { mutableStateOf(TextFieldValue(data.title, TextRange(data.title.length))) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollableState)
                .padding(
                    top = LocalDimens.current.contentPadding,
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding,
                    bottom = 88.dp,
                )
        ) {
            OutlinedTextFieldWithError(
                value = titleState,
                onValueChange = {
                    titleState = it
                    vm.onTitleChanged(titleState.text)
                },
                hint = stringResource(MR.strings.add_article_field_title_hint),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { vm.onNeedToCreateSetPressed() })
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(MR.strings.add_article_create_set_option),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .weight(1.0f)
                )
                Checkbox(
                    checked = data.needToCreateSet,
                    onCheckedChange = null
                )
            }

            OutlinedTextField(
                value = data.text,
                onValueChange = { vm.onTextChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = with(LocalDensity.current) {
                        (42 * 2).sp.toDp()
                    }),
                label = { Text(stringResource(MR.strings.add_article_field_text_hint)) }
            )
        }

        LaunchedEffect("focus") {
            focusRequester.requestFocus()
        }
    }
}

// CustomSnackbar to be able to override text in snackbarData
@Composable
fun CustomSnackbar(
    message: String?,
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = SnackbarDefaults.backgroundColor,
    contentColor: Color = MaterialTheme.colors.surface,
    actionColor: Color = SnackbarDefaults.primaryActionColor,
    elevation: Dp = 6.dp
) {
    val actionLabel = snackbarData.actionLabel
    val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
        @Composable {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                onClick = { snackbarData.performAction() },
                content = { Text(actionLabel) }
            )
        }
    } else {
        null
    }
    Snackbar(
        modifier = modifier.padding(12.dp),
        content = { Text(message ?: snackbarData.message) },
        action = actionComposable,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    )
}