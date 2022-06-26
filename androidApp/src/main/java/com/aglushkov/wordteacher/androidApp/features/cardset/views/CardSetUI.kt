@file:OptIn(ExperimentalAnimationApi::class)

package com.aglushkov.wordteacher.androidApp.features.cardset.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.*
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.shared.events.FocusViewItemEvent
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.toStringDesc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        painter = painterResource(R.drawable.ic_arrow_back_24),
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
                        top = dimensionResource(id = R.dimen.word_horizontalPadding),
                        bottom = 300.dp
                    )
                ) {
                    items(data, key = { it.id }) { item ->
                        CardSetViewItems(
                            Modifier.animateItemPlacement(),
                            item,
                            vm,
                            coroutineScope,
                            focusManager,
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
                    errorText = vm.getErrorText(viewItemsRes)?.resolveString()
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
                        dimensionResource(id = R.dimen.article_horizontalPadding)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_start_learning_24),
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
    coroutineScope: CoroutineScope,
    focusManager: FocusManager,
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
        }
        is WordTranscriptionViewItem -> {
            WordTranscriptionView(
                item,
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
            CardSetDefinitionView(item, item.cardId, vm, coroutineScope)
        } else {
            DeletableCell(
                stateKey = item.id,
                onClick = { /*TODO*/ },
                onDeleted = { vm.onDefinitionRemoved(item, item.cardId) }
            ) {
                CardSetDefinitionView(item, item.cardId, vm, coroutineScope)
            }
        }
        is WordSubHeaderViewItem -> {
            val focusRequester = remember { FocusRequester() }
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
                            }

                            moveFocusDownAfterRecompose(coroutineScope, focusRequester, focusManager)
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
            val focusRequester = remember { FocusRequester() }
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
                            moveFocusDownAfterRecompose(coroutineScope, focusRequester, focusManager)
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
    item: WordDefinitionViewItem,
    cardId: Long,
    vm: CardSetVM,
    coroutineScope: CoroutineScope
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    WordDefinitionView(
        item,
        textContent = { text, textStyle ->
            CardTextField(
                modifier = Modifier
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
                    moveFocusDownAfterRecompose(coroutineScope, focusRequester, focusManager)
                }
            }
        }
    )
}

private fun moveFocusDownAfterRecompose(
    scope: CoroutineScope,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
//    focusRequester.requestFocus()
//    scope.launch {
//        //delay(3000) // TODO: hack to wait until a new cell is rendered
//        //focusManager.moveFocus(FocusDirection.Up)
//    }
}

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
    var textState by remember { mutableStateOf(TextFieldValue(text)) }
    InlineTextField(
        modifier = modifier,
        value = textState,
        placeholder = vm.getPlaceholder(item)?.toString(LocalContext.current).orEmpty(),
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ),
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
                painter = painterResource(id = R.drawable.ic_create_note),
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
                        Text(it.toStringDesc().resolveString())
                    }
                }
        }
    }
}