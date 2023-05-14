package com.aglushkov.wordteacher.android_app.features.learning.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.features.definitions.views.WordDefinitionView
import com.aglushkov.wordteacher.android_app.features.definitions.views.WordExampleView
import com.aglushkov.wordteacher.android_app.features.definitions.views.WordPartOfSpeechView
import com.aglushkov.wordteacher.android_app.features.definitions.views.WordSubHeaderView
import com.aglushkov.wordteacher.android_app.features.definitions.views.WordSynonymView
import com.aglushkov.wordteacher.android_app.general.extensions.resolveString
import com.aglushkov.wordteacher.android_app.general.views.compose.CustomDialogUI
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import java.util.*

@ExperimentalUnitApi
@ExperimentalComposeUiApi
@Composable
fun LearningUIDialog(
    vm: LearningVM,
    modifier: Modifier = Modifier
) {
    CustomDialogUI(
        onDismissRequest = { vm.onClosePressed() }
    ) {
        LearningUI(
            vm = vm,
            modifier = modifier,
            actions = {
                IconButton(
                    onClick = { vm.onClosePressed() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun LearningUI(
    vm: LearningVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val termState by vm.termState.collectAsState()
    val errorString by vm.titleErrorFlow.collectAsState()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()
    val isTestSession = termState.testOptions.isNotEmpty()
    val canShowHint by vm.canShowHint.collectAsState()
    val hintString by vm.hintString.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background),
        ) {
            TopAppBar(
                title = {
                    if (termState.term.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.learning_title)
                                .format(termState.index + 1, termState.count)
                        )
                    }
                },
                actions = actions
            )

            if (data != null) {
                if (isTestSession) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(id = R.dimen.learning_testOption_margin))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OptionButton(vm, termState.testOptions[0])
                                OptionButton(vm, termState.testOptions[1])
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OptionButton(vm, termState.testOptions[2])
                                OptionButton(vm, termState.testOptions[3])
                            }
                        }
                    }
                } else {
                    TermInput(
                        term = termState.term,
                        errorString = errorString?.resolveString(),
                        focusRequester = focusRequester,
                        onDone = { value ->
                            vm.onCheckPressed(value)
                        }
                    )
                    LaunchedEffect(key1 = "editing") {
                        focusRequester.requestFocus()
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = 300.dp
                    )
                ) {
                    items(data, key = { it.id }) { item ->
                        LearningViewItems(itemView = item, vm = vm)
                    }
                }
            } else {
                LoadingStatusView(
                    resource = viewItemsRes,
                    loadingText = null,
                    errorText = vm.getErrorText(viewItemsRes)?.resolveString()
                ) {
                    vm.onTryAgainClicked()
                }
            }
        }

        if (data != null && !isTestSession) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = stringResource(
                                id = if (canShowHint) {
                                    R.string.learning_show_hint
                                } else {
                                    R.string.learning_give_up
                                }
                            ).uppercase(Locale.getDefault()),
                            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.content_padding))
                        )
                    },
                    onClick = {
                        if (canShowHint) {
                            vm.onHintAskedPressed()
                        } else {
                            vm.onGiveUpPressed()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(dimensionResource(id = R.dimen.content_padding))
                )

                SnackbarHost(
                    modifier = Modifier.animateContentSize(),
                    hostState = snackbarHostState
                ) {
                    Snackbar(snackbarData = it)
                }
            }

            LaunchedEffect(key1 = hintString) {
                if (hintString.isNotEmpty()) {
                    val resultString = hintString.fold("") { str, char ->
                        "$str $char"
                    }
                    snackbarHostState.showSnackbar(resultString)
                }
            }
        }
    }
}

@Composable
fun RowScope.OptionButton(
    vm: LearningVM,
    option: String
) {
    OutlinedButton(
        modifier = Modifier
            .weight(1.0f)
            .padding(dimensionResource(id = R.dimen.learning_testOption_margin)),
        onClick = {
            vm.onTestOptionPressed(option)
        }
    ) {
        Text(
            text = option,
            color = MaterialTheme.colors.secondary
        )
    }
}

@Composable
fun TermInput(
    modifier: Modifier = Modifier,
    term: String,
    errorString: String?,
    focusRequester: FocusRequester,
    onDone: (value: String) -> Unit,
) {
    var textValue by remember(term) { mutableStateOf("") }
    val hasError = remember(errorString) { errorString != null }

    Column(
        modifier
            .fillMaxWidth()
            .padding(
                all = dimensionResource(id = R.dimen.learning_horizontalPadding)
            )
    ) {
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(id = R.string.learning_term_title_hint)) },
            isError = hasError,
            trailingIcon = {
                if (hasError) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error_24),
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDone(textValue)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
            )
        )

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 16.dp)
                .padding(horizontal = dimensionResource(id = R.dimen.learning_horizontalPadding))
        ) {
            if (errorString != null) {
                Text(
                    text = errorString,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun LearningViewItems(
    modifier: Modifier = Modifier,
    itemView: BaseViewItem<*>,
    vm: LearningVM,
) {
    when(val item = itemView) {
        is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item, modifier, topPadding = 0.dp)
        is WordDefinitionViewItem -> WordDefinitionView(
            item,
            modifier,
            textContent = { text, ts ->
                Text(
                    modifier = Modifier.weight(1.0f),
                    text = text,
                    style = ts
                )
            }
        )
        is WordSubHeaderViewItem -> WordSubHeaderView(item, modifier)
        is WordSynonymViewItem -> WordSynonymView(item, modifier)
        is WordExampleViewItem -> WordExampleView(item, modifier)
        else -> {
            Text(
                text = "unknown item $item",
                modifier = modifier
            )
        }
    }
}
