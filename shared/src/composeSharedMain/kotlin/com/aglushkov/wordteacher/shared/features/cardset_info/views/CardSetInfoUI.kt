package com.aglushkov.wordteacher.shared.features.cardset_info.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.shared.events.CompletionData
import com.aglushkov.wordteacher.shared.events.CompletionEvent
import com.aglushkov.wordteacher.shared.events.ErrorEvent
import com.aglushkov.wordteacher.shared.features.add_article.views.CustomSnackbar
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.OutlinedTextFieldWithError
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.launch


@Composable
fun CardSetInfoUI(
    vm: CardSetInfoVM,
    modifier: Modifier = Modifier,
) {
    val uiStateRes by vm.uiStateFlow.collectAsState()
    val uiStateData = uiStateRes.data()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background)
            .then(modifier)
    ) {
        Column {
            TopAppBar(
                title = { Text(text = stringResource(MR.strings.cardset_info_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { vm.router?.onClosed() }
                    ) {
                        Icon(
                            painter = painterResource(MR.images.arrow_back_24),
                            contentDescription = null,
                            tint = LocalContentColor.current
                        )
                    }
                },
            )

            if (!uiStateRes.isLoaded() || uiStateData == null) {
                LoadingStatusView(
                    resource = uiStateRes,
                    loadingText = null,
                ) {
                    vm.onTryAgainPressed()
                }
            } else {
                CardSetInfoFieldsUI(vm, uiStateData)
            }
        }
    }
}

@Composable
fun CardSetInfoFieldsUI(vm: CardSetInfoVM, uiState: CardSetInfoVM.UIState) {
    val focusRequester = remember { FocusRequester() }
    val scrollableState = rememberScrollState()
    val focusManager = LocalFocusManager.current
        var nameState by remember { mutableStateOf(TextFieldValue(uiState.name, TextRange(uiState.name.length))) }
    var descriptionState by remember { mutableStateOf(TextFieldValue(uiState.description, TextRange(uiState.description.length))) }
    var sourceState by remember { mutableStateOf(TextFieldValue(uiState.source.orEmpty(), TextRange(uiState.description.length))) }

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
            value = nameState,
            onValueChange = {
                nameState = it
                vm.onNameChanged(it.text)
            },
            hint = stringResource(MR.strings.cardset_info_field_name_hint),
            errorText = uiState.nameError,
            focusRequester = focusRequester,
            focusManager = focusManager,
            readOnly = !uiState.isEditable,
        )

        OutlinedTextField(
            value = descriptionState,
            onValueChange = {
                descriptionState = it
                vm.onDescriptionChanged(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = with(LocalDensity.current) {
                    (42 * 2).sp.toDp()
                }),
            label = { Text(stringResource(MR.strings.cardset_info_field_description_hint)) },
            readOnly = !uiState.isEditable,
        )

        OutlinedTextField(
            value = sourceState,
            onValueChange = {
                sourceState = it
                vm.onSourceChanged(it.text)
            },
            modifier = Modifier
                .fillMaxWidth(),
            label = { Text(stringResource(MR.strings.cardset_info_field_source_hint)) },
            singleLine = true,
            readOnly = !uiState.isEditable,
        )

        if (uiState.isEditable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { vm.onIsAvailableInSearchChanged(!uiState.isAvailableInSearch) })
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(MR.strings.cardset_info_enable_sharing),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .weight(1.0f)
                )
                Checkbox(
                    checked = uiState.isAvailableInSearch,
                    onCheckedChange = null
                )
            }
        }
    }

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }
}