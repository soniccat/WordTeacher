package com.aglushkov.wordteacher.shared.features.cardset.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.events.FocusViewItemEvent
import com.aglushkov.wordteacher.shared.events.ScrollViewItemEvent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.definitions.views.*
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.general.views.DeletableCell
import com.aglushkov.wordteacher.shared.general.views.InlineTextField
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.dpToPx
import com.aglushkov.wordteacher.shared.general.views.pxToDp
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyLevel
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardSetUI(vm: CardSetVM, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val cardSet by vm.cardSet.collectAsState()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()
    val events by vm.eventFlow.collectAsState(initial = emptyList())
    val focusEvent by remember {
        derivedStateOf {
            events.firstOrNull { it is FocusViewItemEvent } as? FocusViewItemEvent
        }
    }
    val scrollEvent by remember {
        derivedStateOf {
            events.firstOrNull { it is ScrollViewItemEvent } as? ScrollViewItemEvent
        }
    }
    val scrollOffset = -50.dpToPx().toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = cardSet.data()?.name.orEmpty(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { vm.onBackPressed() }
                ) {
                    Icon(
                        painter = painterResource(MR.images.arrow_back_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            },
            actions = {
                if (vm.availableFeatures.canEnrich && !state.isRemoteCardSet) {
                    Button(
                        onClick = { vm.onEnrichClicked() }
                    ) {
                        Text(text = "Enrich")
                    }
                }

                IconButton(
                    onClick = { vm.onInfoPressed() }
                ) {
                    Icon(
                        painter = painterResource(MR.images.info_24),
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        )

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            if (data != null) {
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = LocalDimensWord.current.wordHorizontalPadding,
                        bottom = 300.dp
                    )
                ) {
                    items(data, key = { it.id }) { item ->
                        CardSetViewItems(
                            Modifier.animateItemPlacement(),
                            item,
                            vm,
                            if (item == focusEvent?.viewItem) {
                                focusEvent?.markAsHandled()
                                focusEvent
                            } else {
                                null
                            },
                            isEditable = !state.isRemoteCardSet,
                            onFocused = {
                                if (!state.isRemoteCardSet) {
                                    coroutineScope.launch {
                                        val index = data.indexOf(it)
                                        if (index != -1) {
                                            listState.animateScrollToItem(index, scrollOffset)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                if (scrollEvent != null) {
                    LaunchedEffect(key1 = "scroll") {
                        val index = data.indexOf(scrollEvent?.viewItem)
                        if (index != -1) {
                            scrollEvent?.markAsHandled()
                            listState.animateScrollToItem(index, scrollOffset)
                        }
                    }
                }
            } else {
                LoadingStatusView(
                    resource = viewItemsRes,
                    loadingText = null,
                    errorText = vm.getErrorText(viewItemsRes)?.localized()
                ) {
                    vm.onTryAgainClicked()
                }
            }

            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (state.isRemoteCardSet) {
                    FloatingActionButton(
                        onClick = { vm.onAddClicked() },
                        modifier = Modifier.padding(
                            LocalDimensWord.current.articleHorizontalPadding
                        )
                    ) {
                        Icon(
                            painter = painterResource(MR.images.add_white_24),
                            contentDescription = null
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = { vm.onStartLearningClicked() },
                        modifier = Modifier.padding(
                            LocalDimensWord.current.articleHorizontalPadding
                        )
                    ) {
                        Icon(
                            painter = painterResource(MR.images.start_learning_24),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardSetViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: CardSetVM,
    focusEvent: FocusViewItemEvent?,
    isEditable: Boolean,
    onFocused: (itemView: BaseViewItem<*>) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    when (val item = itemView) {
        is WordTitleViewItem -> {
            DeletableCell(
                stateKey = item.id,
                enabled = isEditable,
                onClick = { /*TODO*/ },
                onDeleted = { vm.onCardDeleted(item.cardId) }
            ) {
                WordTitleView(
                    viewItem = item,
                    textContent = { text, textStyle ->
                        CardItemTextField(
                            Modifier.onFocusChanged {
                                    if (it.isFocused) {
                                        onFocused(item)
                                    }
                                }
                                .weight(1.0f),
                            text,
                            textStyle,
                            item,
                            item.cardId,
                            !isEditable,
                            vm,
                        )
                    },
                )
            }
        }

        is WordTranscriptionViewItem -> {
            WordTranscriptionView(
                item,
                modifier = Modifier.focusRequester(focusRequester),
                textContent = { text, textStyle ->
                    CardItemTextField(
                        Modifier.onFocusChanged {
                            if (it.isFocused) {
                                onFocused(item)
                            }
                        },
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        !isEditable,
                        vm,
                    )
                }
            )
        }

        is WordAudioFilesViewItem -> WordAudioFilesView(
            item,
            Modifier.padding(
                start = LocalDimensWord.current.wordHorizontalPadding,
                end = LocalDimensWord.current.wordHorizontalPadding,
            )
        ) {
            vm.onAudioFileClicked(it)
        }

        is WordPartOfSpeechViewItem -> PartOfSpeechSelectPopup(
            vm,
            item,
            item.cardId
        ) { onClicked ->
            WordPartOfSpeechView(
                item,
                modifier = Modifier
                    .clickable(
                        enabled = isEditable,
                        onClick = onClicked
                    )
                    .focusable(false)
            )
        }

        is WordDefinitionViewItem -> if (item.isLast && item.index == 0) {
            CardSetDefinitionView(
                Modifier,
                item,
                item.cardId,
                isEditable,
                vm,
                focusRequester,
                focusEvent?.elementIndex,
                onFocused = { onFocused(item) }
            )
        } else {
            DeletableCell(
                stateKey = item.id,
                enabled = isEditable,
                onClick = { /*TODO*/ },
                onDeleted = { vm.onDefinitionRemoved(item, item.cardId) }
            ) {
                CardSetDefinitionView(
                    Modifier,
                    item,
                    item.cardId,
                    isEditable,
                    vm,
                    focusRequester,
                    focusEvent?.elementIndex,
                    onFocused = { onFocused(item) }
                )
            }
        }

        is WordSubHeaderViewItem -> {
            WordSubHeaderView(
                item,
                modifier = Modifier.focusRequester(focusRequester),
                textContent = { text, ts ->
                    Text(
                        text = text,
                        style = ts,
                        modifier = Modifier.weight(1.0f),
                    )
                    if (item.isOnlyHeader && isEditable) {
                        AddIcon {
                            when (item.contentType) {
                                WordSubHeaderViewItem.ContentType.SYNONYMS -> vm.onAddSynonymPressed(
                                    item.cardId
                                )
                                WordSubHeaderViewItem.ContentType.EXAMPLES -> vm.onAddExamplePressed(
                                    item.cardId
                                )
                                else -> {}
                            }
                        }
                    }
                }
            )
        }

        is WordSynonymViewItem -> DeletableCell(
            stateKey = item.id,
            enabled = isEditable,
            onClick = { /*TODO*/ },
            onDeleted = { vm.onSynonymRemoved(item, item.cardId) }
        ) {
            WordSynonymView(
                item,
                textContent = { text, textStyle ->
                    CardItemTextField(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.isFocused) {
                                    onFocused(item)
                                }
                            }
                            .weight(1.0f)
                            .focusRequester(focusRequester),
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        !isEditable,
                        vm
                    )

                    if (item.isLast && isEditable) {
                        AddIcon {
                            vm.onAddSynonymPressed(item.cardId)
                        }
                    }
                }
            )
        }

        is WordExampleViewItem -> DeletableCell(
            stateKey = item.id,
            enabled = isEditable,
            onClick = { /*TODO*/ },
            onDeleted = { vm.onExampleRemoved(item, item.cardId) }
        ) {
            WordExampleView(
                item,
                textContent = { text, textStyle ->
                    CardItemTextField(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.isFocused) {
                                    onFocused(item)
                                }
                            }
                            .weight(1.0f)
                            .focusRequester(focusRequester),
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        !isEditable,
                        vm
                    )

                    if (item.isLast && isEditable) {
                        AddIcon {
                            vm.onAddExamplePressed(item.cardId)
                        }
                    }
                }
            )
        }

        is CreateCardViewItem -> CreateCardView(
            item,
            modifier,
            onClicked = {
                vm.onCardCreatePressed()
            }
        )

        is WordDividerViewItem -> WordDividerView()
        else -> {
            Text(
                text = "unknown item $item",
                modifier = modifier
            )
        }
    }

    if (focusEvent != null) {
        LaunchedEffect(key1 = "focus") {
            focusRequester.requestFocus()
        }
    }
}

//@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
//@Composable
//private fun CardView(
//    cardItem: CardViewItem,
//    vm: CardSetVM,
//    coroutineScope: CoroutineScope
//) {
//    val focusManager = LocalFocusManager.current
//    val cardId = cardItem.cardId
//    cardItem.innerViewItems.onEach {
//        when (val item = it) {
//
//        }
//    }
//}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardSetDefinitionView(
    modifier: Modifier = Modifier,
    item: WordDefinitionViewItem,
    cardId: Long,
    isEditable: Boolean,
    vm: CardSetVM,
    focusRequester: FocusRequester,
    focusIndex: Int? = null,
    onFocused: (() -> Unit)? = null
) {
    WordDefinitionView(
        item,
        textContent = { text, textStyle ->
            CardItemTextField(
                modifier = modifier
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocused?.invoke()
                        }
                    }
                    .weight(1.0f)
                    .let {
                        if (focusIndex == 0) {
                            it.focusRequester(focusRequester)
                        } else {
                            it
                        }
                    },
                text,
                textStyle,
                item,
                cardId,
                !isEditable,
                vm,
            )

            if (item.isLast && isEditable) {
                AddIcon {
                    vm.onAddDefinitionPressed(cardId)
                }
            }
        },
        labelContent = { text, index ->
            CardTextField(
                modifier = Modifier
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocused?.invoke()
                        }
                    }
                    .padding(start = 4.dp, end = if (isEditable) 0.dp else 4.dp)
                    .width(IntrinsicSize.Min)
                    .let {
                        if (index == item.labels.size - 1 && focusIndex == 1) {
                            it.focusRequester(focusRequester)
                        } else {
                            it
                        }
                    },
                text = text,
                placeholder = "",
                readOnly = !isEditable,
                onValueChanged = { newText ->
                    vm.onLabelTextChanged(newText, index, cardId)
                }
            )
            if (isEditable) {
                Icon(
                    painter = painterResource(MR.images.close_18),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(start = 4.dp)
                        .clickable {
                            vm.onLabelDeleted(index, cardId)
                        },
                    tint = MaterialTheme.colors.onSecondary
                )
            }
        },
        lastLabel = if (item.showAddLabel) {
            {
                if (item.labels.isEmpty()) {
                    CustomBadge(
                        modifier = Modifier.clickable {
                            vm.onAddLabelPressed(cardId)
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
                        vm.onAddLabelPressed(cardId)
                    }
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun CardItemTextField(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = LocalTextStyle.current,
    item: BaseViewItem<*>,
    cardId: Long,
    readOnly: Boolean,
    vm: CardSetVM,
) {
    CardTextField(
        modifier = modifier,
        text = text,
        textStyle = textStyle,
        placeholder = vm.getPlaceholder(item)?.localized().orEmpty(),
        readOnly = readOnly,
        onValueChanged = {
            vm.onItemTextChanged(it, item, cardId)
        }
    )
}

@Composable
private fun CardTextField(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: String = "",
    readOnly: Boolean,
    onValueChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    InlineTextField(
        modifier = modifier,
        value = textState,
        placeholder = placeholder,
        textStyle = if (!textStyle.color.isSpecified) {
            textStyle.copy(
                color = LocalContentColor.current
            )
        } else {
            textStyle
        },
        focusManager = focusManager,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        readOnly = readOnly,
        onValueChange = {
            if (!readOnly) { // filter cursor position change in readOnly mode
                val hasTextChanged = textState.text != it.text
                textState = it
                if (hasTextChanged) {
                    onValueChanged(it.text)
                }
//                vm.onItemTextChanged(it.text, item, cardId)
            }
        }
    )
}

@Composable
private fun CreateCardView(
    viewItem: CreateCardViewItem,
    modifier: Modifier,
    onClicked: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        IconButton(
            onClick = onClicked
        ) {
            Icon(
                painter = painterResource(MR.images.create_note),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun PartOfSpeechSelectPopup(
    vm: CardSetVM,
    item: WordPartOfSpeechViewItem,
    cardId: Long,
    content: @Composable (onClicked: () -> Unit) -> Unit
) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        content { expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WordTeacherWord.PartOfSpeech.values()
                .filter { it != item.partOfSpeech }
                .onEach {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            vm.onPartOfSpeechChanged(it, cardId)
                        }
                    ) {
                        Text(it.toStringDesc().localized())
                    }
                }
        }
    }
}

