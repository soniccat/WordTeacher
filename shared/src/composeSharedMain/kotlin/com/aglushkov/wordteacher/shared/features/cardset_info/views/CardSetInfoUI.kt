package com.aglushkov.wordteacher.shared.features.cardset_info.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.shared.features.cardset.views.CardTextField
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVM
import com.aglushkov.wordteacher.shared.features.cardset_info.vm.CardSetInfoVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.views.CustomBadge
import com.aglushkov.wordteacher.shared.features.definitions.views.WordLabels
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.isLoading
import com.aglushkov.wordteacher.shared.general.resource.on
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.general.views.DownloadForOfflineButton
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.OutlinedTextFieldWithError
import com.aglushkov.wordteacher.shared.general.views.listBottomPadding
import com.aglushkov.wordteacher.shared.general.views.windowInsetsHorizontalPadding
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
fun CardSetInfoFieldsUI(
    vm: CardSetInfoVM,
    uiState: CardSetInfoVM.UIState,
) {
    val focusRequester = remember { FocusRequester() }
    val scrollableState = rememberScrollState()
    val focusManager = LocalFocusManager.current
        var nameState by remember { mutableStateOf(TextFieldValue(uiState.name, TextRange(uiState.name.length))) }
    var descriptionState by remember { mutableStateOf(TextFieldValue(uiState.description, TextRange(uiState.description.length))) }
    var sourceState by remember {
        mutableStateOf(TextFieldValue(uiState.source.orEmpty(),
            TextRange(uiState.source.orEmpty().length))) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsHorizontalPadding()
            .verticalScroll(scrollableState)
            .padding(
                top = LocalDimens.current.contentPadding,
                bottom = listBottomPadding(),
            )
    ) {
        OutlinedTextFieldWithError(
            value = nameState,
            onValueChange = {
                nameState = it
                vm.onNameChanged(it.text)
            },
            modifier = Modifier.padding(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
            ),
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
                .padding(
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding,
                )
                .fillMaxWidth()
                .sizeIn(minHeight = with(LocalDensity.current) {
                    (42 * 2).sp.toDp()
                }),
            label = { Text(stringResource(MR.strings.cardset_info_field_description_hint)) },
            readOnly = !uiState.isEditable,
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = sourceState,
            onValueChange = {
                sourceState = it
                vm.onSourceChanged(it.text)
            },
            modifier = Modifier
                .padding(
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding,
                )
                .fillMaxWidth(),
            label = { Text(stringResource(MR.strings.cardset_info_field_source_hint)) },
            readOnly = !uiState.isEditable,
        )

        uiState.sourceLinks.onEach { link ->
            Row(
                Modifier.padding(
                    top = 6.dp,
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding
                )
            ) {
                val linkString = uiState.source.orEmpty().substring(link.span.start, link.span.end)
                Text(
                    text = linkString,
                    modifier = Modifier.weight(1.0f).clickable {
                        vm.onLinkClicked(linkString)
                    }.padding(6.dp),
                    color = MaterialTheme.colors.secondary
                )
                if (link.canImport) {
                    DownloadForOfflineButton(
                        modifier = Modifier.clickable {
                            vm.onImportArticleClicked(linkString)
                        }
                    )
//                    Icon(
//                        painterResource(MR.images.download_for_offline),
//                        null,
//                        modifier = Modifier
//                            .padding(top = 6.dp)
//                            .clickable {
//                            vm.onImportArticleClicked(linkString)
//                        },
//                        tint = MaterialTheme.colors.secondary
//                    )
                }
            }
        }

        if (uiState.isEditable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { vm.onIsAvailableInSearchChanged(!uiState.isAvailableInSearch) })
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp,
                        start = LocalDimens.current.contentPadding,
                        end = LocalDimens.current.contentPadding,
                    )
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

        // tags
        if (uiState.tags.isNotEmpty() || uiState.isEditable) {
            WordLabels(
                uiState.tags,
                modifier = Modifier.padding(start = 10.dp, end = 24.dp),
                textContent = { text, index ->
                    CardTextField(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.isFocused) {
//                                    if (uiState.isEditable) {
//                                        coroutineScope.launch {
//                                            val index = data.indexOf(it)
//                                            if (index != -1) {
//                                                listState.animateScrollToItem(index, scrollOffset)
//                                            }
//                                        }
//                                    }
                                }
                            }
                            .padding(start = 4.dp, end = if (uiState.isEditable) 0.dp else 4.dp)
                            .width(IntrinsicSize.Min)
                            .let {
                                val event = uiState.focusEvent as? CardSetInfoVMImpl.FocusLastEvent
                                if (index == uiState.tags.size - 1 && event?.type == CardSetInfoVMImpl.ElementType.Tag) {
                                    vm.onFocusEventHandled()
                                    it.focusRequester(focusRequester)
                                } else {
                                    it
                                }
                            },
                        text = text,
                        placeholder = "",
                        readOnly = !uiState.isEditable,
                        onValueChanged = { newText ->
                            vm.onTagTextChanged(newText, index)
                        }
                    )
                    if (uiState.isEditable) {
                        Icon(
                            painter = painterResource(MR.images.close_18),
                            contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .padding(start = 4.dp)
                                .clickable {
                                    vm.onTagDeleted(index)
                                },
                            tint = MaterialTheme.colors.onSecondary
                        )
                    }
                },
                lastItem = if (uiState.isEditable) {
                    {
                        if (uiState.tags.isNotEmpty()) {
                            CustomBadge(
                                modifier = Modifier.clickable {
                                    vm.onAddTagPressed()
                                }.align(Alignment.CenterVertically).padding(horizontal = 2.dp),
                                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colors.onSecondary,
                                content = {
                                    Text(
                                        text = stringResource(MR.strings.cardset_add_label),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            )
                        } else {
                            AddIcon(modifier = Modifier.align(Alignment.CenterVertically)) {
                                vm.onAddTagPressed()
                            }
                        }
                    }
                } else {
                    null
                }
            )
        }
    }

    if (uiState.focusEvent != null) {
        LaunchedEffect("focus") {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }
}
