package com.aglushkov.wordteacher.androidApp.features.add_article.views

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomDialogUI
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalUnitApi
@ExperimentalComposeUiApi
@Composable
fun AddArticleUI(
    vm: AddArticleVM,
    onDismissRequest: () -> Unit
) {
    CustomDialogUI(
        onDismissRequest = onDismissRequest
    ) {
        val context = LocalContext.current

        val title by vm.title.collectAsState()
        val titleError by vm.titleErrorFlow.collectAsState(initial = null)
        val text by vm.text.collectAsState()

        val scrollableState = rememberScrollState()
        val snackbarHostState = remember { SnackbarHostState() }
        var wasTitleFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        Column {
            TopAppBar(
                title = { Text(stringResource(id = R.string.add_article_title)) },
                actions = {
                    IconButton(
                        onClick = {vm.onCancelPressed()}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                }
            )

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
                    isError = titleError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    ),
                    singleLine = true
                )

                Box(
                    modifier = Modifier.height(30.dp)
                ) {
                    val titleErrorDesc = titleError
                    if (titleErrorDesc != null) {
                        Text(
                            titleErrorDesc.toString(LocalContext.current),
                            color = MaterialTheme.colors.error
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { vm.onTextChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.add_article_field_text_hint)) }
                )
            }
        }

        ExtendedFloatingActionButton(
            text = {
                Text(
                    text = stringResource(id = R.string.add_article_done),
                )
           },
            onClick = { vm.onCompletePressed() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(id = R.dimen.content_padding))
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Snackbar(
                snackbarData = it
            )
        }

        LaunchedEffect("focus") {
            focusRequester.requestFocus()

            vm.eventFlow.collect {
                when (it) {
                    is CompletionEvent -> onDismissRequest()
                    is ErrorEvent -> {
                        launch {
                            snackbarHostState.showSnackbar(it.text.toString(context))
                        }
                    }
                }
            }
        }
    }
}
