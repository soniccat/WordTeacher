package com.aglushkov.wordteacher.androidApp.features.cardset.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.*
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardViewItem
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Card

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardSetUI(vm: CardSetVM, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val cardSet by vm.cardSet.collectAsState()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()

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

        if (data != null) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = dimensionResource(id = R.dimen.word_horizontalPadding),
                )
            ) {
                items(data, key = { it.id }) { item ->
                    CardSetViewItems(Modifier.animateItemPlacement(), item, vm)
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
}

@Composable
fun CardSetViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: CardSetVM,
) {
    when(val item = itemView) {
        is CardViewItem -> CardView(item, vm)
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
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun CardView(
    cardItem: CardViewItem,
    vm: CardSetVM
) {
    val card = cardItem.card
    DeletableCell(
        stateKey = cardItem.id,
        onClick = { /*TODO*/ },
        onDeleted = { vm.onCardDeleted(cardItem.card) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            cardItem.innerViewItems.onEach {
                when (val item = it) {
                    is WordTitleViewItem -> {
                        WordTitleView(
                            viewItem = item,
                            textContent = { text, textStyle ->
                                CardTextField(Modifier, text, textStyle, item, card, vm)
                            }
                        )
                    }
                    is WordTranscriptionViewItem -> {
                        WordTranscriptionView(
                            item,
                            textContent = { text, textStyle ->
                                CardTextField(Modifier, text, textStyle, item, card, vm)
                            }
                        )
                    }
                    is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item)
                    is WordDefinitionViewItem -> DeletableCell(
                            stateKey = item.id,
                            onClick = { /*TODO*/ },
                            onDeleted = { vm.onDefinitionRemoved(item, card) }
                        ) {
                            WordDefinitionView(
                                item,
                                textContent = { text, textStyle ->
                                    CardTextField(
                                        modifier = Modifier.weight(1.0f),
                                        text,
                                        textStyle,
                                        item,
                                        card,
                                        vm
                                    )

                                    val needShowAddIcon = item.index == card.definitions.size - 1
                                    if (needShowAddIcon) {
                                        AddIcon {
                                            vm.onAddDefinitionPressed(card)
                                        }
                                    }
                                }
                            )
                        }
                    is WordSubHeaderViewItem -> WordSubHeaderView(item)
                    is WordSynonymViewItem -> DeletableCell(
                        stateKey = item.id,
                        onClick = { /*TODO*/ },
                        onDeleted = { vm.onSynonymRemoved(item, card) }
                    ) {
                        WordSynonymView(
                            item,
                            textContent = { text, textStyle ->
                                CardTextField(
                                    modifier = Modifier.weight(1.0f),
                                    text,
                                    textStyle,
                                    item,
                                    card,
                                    vm
                                )

                                val needShowAddIcon = item.index == card.synonyms.size - 1
                                if (needShowAddIcon) {
                                    AddIcon {
                                        vm.onAddSynonymPressed(card)
                                    }
                                }
                            }
                        )
                    }
                    is WordExampleViewItem -> DeletableCell(
                        stateKey = item.id,
                        onClick = { /*TODO*/ },
                        onDeleted = { vm.onExampleRemoved(item, card) }
                    ) {
                        WordExampleView(
                            item,
                            textContent = { text, textStyle ->
                                CardTextField(
                                    modifier = Modifier.weight(1.0f),
                                    text,
                                    textStyle,
                                    item,
                                    card,
                                    vm
                                )

                                val needShowAddIcon = item.index == card.examples.size - 1
                                if (needShowAddIcon) {
                                    AddIcon {
                                        vm.onAddExamplePressed(card)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTextField(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    item: BaseViewItem<*>,
    card: Card,
    vm: CardSetVM
) {
    var textState by remember { mutableStateOf(TextFieldValue(text)) }
    InlineTextField(
        modifier = modifier,
        value = textState,
        placeholder = vm.getPlaceholder(item)?.toString(LocalContext.current).orEmpty(),
        textStyle = textStyle,
        onValueChange = {
            textState = it
            vm.onItemTextChanged(it.text, item, card)
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
