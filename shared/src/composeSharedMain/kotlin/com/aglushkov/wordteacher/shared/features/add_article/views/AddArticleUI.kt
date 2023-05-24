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
import androidx.compose.ui.text.input.ImeAction
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
    val focusRequester = remember { FocusRequester() }
    val snackbarStringDesc = remember { mutableStateOf<dev.icerock.moko.resources.desc.StringDesc?>(null) }

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
                focusRequester = focusRequester
            )
        }

        ExtendedFloatingActionButton(
            text = {
                Text(
                    text = stringResource(MR.strings.add_article_done),
                    modifier = Modifier.padding(horizontal = LocalDimens.current.contentPadding)
                )
            },
            onClick = { vm.onCompletePressed() },
            modifier = Modifier.Companion
                .align(Alignment.BottomEnd)
                .padding(LocalDimens.current.contentPadding)
        )

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

    LaunchedEffect("focus") {
        focusRequester.requestFocus()

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
    focusRequester: FocusRequester
) {
    val title by vm.title.collectAsState()
    val titleError by vm.titleErrorFlow.collectAsState(initial = null)
    val text by vm.text.collectAsState()
    val needToCreateSet by vm.needToCreateSet.collectAsState()

    val hasTitleError = remember(titleError) { titleError != null }
    val scrollableState = rememberScrollState()
    var wasTitleFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
        OutlinedTextField(
            value = title,
            onValueChange = { vm.onTitleChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        wasTitleFocused = true
                    }

                    if (wasTitleFocused) {
                        vm.onTitleFocusChanged(it.isFocused)
                    }
                },
            label = { Text(stringResource(MR.strings.add_article_field_title_hint)) },
            isError = hasTitleError,
            trailingIcon = {
                if (hasTitleError) {
                    Icon(
                        painter = painterResource(MR.images.error_24),
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            singleLine = true
        )

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            val titleErrorDesc = titleError
            if (titleErrorDesc != null) {
                Text(
                    titleErrorDesc.localized(),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }

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
                checked = needToCreateSet,
                onCheckedChange = null
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = { vm.onTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = with(LocalDensity.current) {
                    (42 * 2).sp.toDp()
                }),
            label = { Text(stringResource(MR.strings.add_article_field_text_hint)) }
        )
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