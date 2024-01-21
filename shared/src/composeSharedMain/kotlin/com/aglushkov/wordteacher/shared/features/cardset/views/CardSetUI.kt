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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.events.FocusViewItemEvent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.definitions.views.*
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.DropdownMenu
import com.aglushkov.wordteacher.shared.general.DropdownMenuItem
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.AddIcon
import com.aglushkov.wordteacher.shared.general.views.DeletableCell
import com.aglushkov.wordteacher.shared.general.views.InlineTextField
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradation
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyLevel
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.localized

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardSetUI(vm: CardSetVM, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val cardSet by vm.cardSet.collectAsState()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()
    val focusManager = LocalFocusManager.current
    val events by vm.eventFlow.collectAsState(initial = emptyList())
    val focusEvent by remember {
        derivedStateOf {
            events.firstOrNull { it is FocusViewItemEvent } as? FocusViewItemEvent
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(text = cardSet.data()?.name.orEmpty())
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
            }
        )

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            if (data != null) {
                LazyColumn(
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
                                true
                            } else {
                                false
                            }
                        )
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun CardSetViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: CardSetVM,
    needFocus: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    when(val item = itemView) {
        is WordTitleViewItem -> {
            DeletableCell(
                stateKey = item.id,
                onClick = { /*TODO*/ },
                onDeleted = { vm.onCardDeleted(item.cardId) }
            ) {
                WordTitleView(
                    viewItem = item,
                    textContent = { text, textStyle ->
                        CardTextField(
                            Modifier.weight(1.0f),
                            text,
                            textStyle,
                            item,
                            item.cardId,
                            vm,
                        )
                        if (item.frequencyLevel != null) {
                            Box(
                                Modifier
                                    .size(40.dp).padding(10.dp)
                                    .run {
                                        if (item.frequencyLevel != WordFrequencyGradation.UNKNOWN_LEVEL) {
                                            background(
                                                color = wordFrequencyColor(item.frequencyRatio),
                                                shape = RoundedCornerShape(30.dp)
                                            )
                                        } else {
                                            this
                                        }
                                    }
                                    .border(
                                        1.dp,
                                        color = StartWordFrequencyColor,
                                        shape = RoundedCornerShape(30.dp)
                                    ),
                            ) {
                                Text(
                                    text = "${ if (item.frequencyLevel == WordFrequencyGradation.UNKNOWN_LEVEL) "?" else item.frequencyLevel}",
                                    modifier = Modifier.fillMaxSize(),
                                    textAlign = TextAlign.Center,
                                    style = LocalAppTypography.current.wordFrequency
                                )
                            }
                        }
                    }
                )
            }
        }
        is WordTranscriptionViewItem -> {
            WordTranscriptionView(
                item,
                modifier = Modifier.focusRequester(focusRequester),
                textContent = { text, textStyle ->
                    CardTextField(
                        Modifier,
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        vm,
                    )
                }
            )
        }
        is WordPartOfSpeechViewItem -> PartOfSpeechSelectPopup(
            vm,
            item,
            item.cardId
        ) { onClicked ->
            WordPartOfSpeechView(
                item,
                modifier = Modifier
                    .clickable(onClick = onClicked)
                    .focusable(false)
            )
        }
        is WordDefinitionViewItem -> if (item.isLast && item.index == 0) {
            CardSetDefinitionView(Modifier, item, item.cardId, vm, focusRequester)
        } else {
            DeletableCell(
                stateKey = item.id,
                onClick = { /*TODO*/ },
                onDeleted = { vm.onDefinitionRemoved(item, item.cardId) }
            ) {
                CardSetDefinitionView(Modifier, item, item.cardId, vm, focusRequester)
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
                    if (item.isOnlyHeader) {
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
            onClick = { /*TODO*/ },
            onDeleted = { vm.onSynonymRemoved(item, item.cardId) }
        ) {
            WordSynonymView(
                item,
                textContent = { text, textStyle ->
                    CardTextField(
                        modifier = Modifier
                            .weight(1.0f)
                            .focusRequester(focusRequester),
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        vm
                    )

                    if (item.isLast) {
                        AddIcon {
                            vm.onAddSynonymPressed(item.cardId)
                        }
                    }
                }
            )
        }
        is WordExampleViewItem -> DeletableCell(
            stateKey = item.id,
            onClick = { /*TODO*/ },
            onDeleted = { vm.onExampleRemoved(item, item.cardId) }
        ) {
            WordExampleView(
                item,
                textContent = { text, textStyle ->
                    CardTextField(
                        modifier = Modifier
                            .weight(1.0f)
                            .focusRequester(focusRequester),
                        text,
                        textStyle,
                        item,
                        item.cardId,
                        vm
                    )

                    if (item.isLast) {
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

    if (needFocus) {
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

@Composable
private fun CardSetDefinitionView(
    modifier: Modifier = Modifier,
    item: WordDefinitionViewItem,
    cardId: Long,
    vm: CardSetVM,
    focusRequester: FocusRequester
) {
    WordDefinitionView(
        item,
        textContent = { text, textStyle ->
            CardTextField(
                modifier = modifier
                    .weight(1.0f)
                    .focusRequester(focusRequester),
                text,
                textStyle,
                item,
                cardId,
                vm,
            )

            if (item.isLast) {
                AddIcon {
                    vm.onAddDefinitionPressed(cardId)
                }
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CardTextField(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    item: BaseViewItem<*>,
    cardId: Long,
    vm: CardSetVM
) {
    val focusManager = LocalFocusManager.current
    var textState by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    InlineTextField(
        modifier = modifier,
        value = textState,
        placeholder = vm.getPlaceholder(item)?.localized().orEmpty(),
        textStyle = textStyle.copy(
            color = LocalContentColor.current
        ),
        focusManager = focusManager,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onValueChange = {
            textState = it
            vm.onItemTextChanged(it.text, item, cardId)
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

fun wordFrequencyColor(ratio: Float?): Color {
    if (ratio == null) {
        return Color.Transparent
    }

    return StartWordFrequencyColor.copy(alpha = 1 - ratio)
//    return lerp(
//        StartWordFrequencyColor,
//        EndWordFrequencyColor,
//        level,
//    )
}

private val StartWordFrequencyColor = Color(0xFF60D838)
private val EndWordFrequencyColor = Color(0xFFFF634D)
