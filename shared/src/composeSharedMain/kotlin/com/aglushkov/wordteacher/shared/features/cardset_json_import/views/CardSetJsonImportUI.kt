package com.aglushkov.wordteacher.shared.features.cardset_json_import.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.events.CompletionData
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.CardSetJsonImportVM
import com.aglushkov.wordteacher.shared.general.CustomDialogUI
import com.aglushkov.wordteacher.shared.general.LocalDimens
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

@Composable
fun CardSetJsonImportUIDialog(
    vm: CardSetJsonImportVM,
    modifier: Modifier = Modifier,
    onCardSetCreated: () -> Unit
) {
    CustomDialogUI(
        onDismissRequest = { onCardSetCreated() }
    ) {
        CardSetJsonImportUI(
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
            { onCardSetCreated() }
        )
    }
}

@Composable
fun CardSetJsonImportUI(
    vm: CardSetJsonImportVM,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    onCardSetCreated: SnackbarHostState.(cardSetId: Long?) -> Unit
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
                title = { Text(text = stringResource(MR.strings.cardset_json_import)) },
                actions = actions
            )

            CardSetJsonImportFieldsUI(
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
                    onCardSetCreated((it.data as? CompletionData.Article)?.id)
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
private fun CardSetJsonImportFieldsUI(
    vm: CardSetJsonImportVM,
    focusRequester: FocusRequester
) {
    val jsonText by vm.jsonText.collectAsState()
    val jsonTextError by vm.jsonTextErrorFlow.collectAsState(initial = null)
    val scrollableState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollableState)
            .focusRequester(focusRequester)
            .padding(
                top = LocalDimens.current.contentPadding,
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
                bottom = 88.dp,
            )
    ) {
        OutlinedTextField(
            value = jsonText,
            onValueChange = { vm.onJsonTextChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = with(LocalDensity.current) {
                    (42 * 2).sp.toDp()
                }),
            label = { Text(stringResource(MR.strings.add_article_field_text_hint)) }
        )
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            val titleErrorDesc = jsonTextError
            if (titleErrorDesc != null) {
                Text(
                    titleErrorDesc.localized(),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }
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