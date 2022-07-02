package com.aglushkov.wordteacher.androidApp.features.add_article.views

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomDialogUI
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import kotlinx.coroutines.launch

@ExperimentalUnitApi
@ExperimentalComposeUiApi
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
                        painter = painterResource(R.drawable.ic_close_24),
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
    onArticleCreated: SnackbarHostState.() -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Column {
            TopAppBar(
                title = { Text(stringResource(id = R.string.add_article_title)) },
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
                    text = stringResource(id = R.string.add_article_done),
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.content_padding))
                )
            },
            onClick = { vm.onCompletePressed() },
            modifier = Modifier.Companion
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(id = R.dimen.content_padding))
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.Companion.align(Alignment.BottomCenter)
        ) {
            Snackbar(
                snackbarData = it
            )
        }
    }

    LaunchedEffect("focus") {
        focusRequester.requestFocus()

        vm.eventFlow.collect {
            when (it) {
                is CompletionEvent -> with(snackbarHostState) {
                // TODO: handle cancellation
                    onArticleCreated()
                }
                is ErrorEvent -> {
                    launch {
                        snackbarHostState.showSnackbar(it.text.toString(context))
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
                top = dimensionResource(id = R.dimen.content_padding),
                start = dimensionResource(id = R.dimen.content_padding),
                end = dimensionResource(id = R.dimen.content_padding),
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
            label = { Text(stringResource(id = R.string.add_article_field_title_hint)) },
            isError = hasTitleError,
            trailingIcon = {
                if (hasTitleError) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error_24),
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
                    titleErrorDesc.toString(LocalContext.current),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { vm.onTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = with(LocalDensity.current) {
                    (42 * 2).sp.toDp()
                }),
            label = { Text(stringResource(id = R.string.add_article_field_text_hint)) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { vm.onNeedToCreateSetPressed() })
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.add_article_create_set_option),
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1.0f)
            )
            Checkbox(
                checked = needToCreateSet,
                onCheckedChange = null
            )
        }
    }
}
