package com.aglushkov.wordteacher.androidApp.features.cardsets.views

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.*
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetViewItem
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CreateCardSetViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.isLoaded

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun CardSetsUI(
    vm: CardSetsVM,
    modifier: Modifier = Modifier
) {
    val cardSets by vm.cardSets.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val data = cardSets.data()

    val newCardSetTextState = vm.stateFlow.collectAsState()
    val newCardSetState by remember { mutableStateOf(TextFieldCellStateImpl { newCardSetTextState.value.newCardSetText }) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column{
            CustomTopAppBar {
                SearchView(searchText, { searchText = it }) {
                    //vm.onWordSubmitted(searchText)
                }
            }

            if (cardSets.isLoaded() && data != null) {
                LazyColumn {
                    items(
                        data,
                        key = { it.id }
                    ) { item ->
                        CardSetsViewItem(item, vm, newCardSetState)
                    }
                }
            } else {
                LoadingStatusView(
                    resource = cardSets,
                    loadingText = null,
                    errorText = vm.getErrorText(cardSets)?.resolveString()
                ) {
                    vm.onTryAgainClicked()
                }
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

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun CardSetsViewItem(
    item: BaseViewItem<*>,
    vm: CardSetsVM,
    newCardSetState: TextFieldCellState,
) = when (item) {
    is CreateCardSetViewItem -> TextFieldCellView(
        placeholder = item.placeholder.toString(LocalContext.current),
        textFieldValue = newCardSetState.rememberTextFieldValueState(),
        focusRequester = newCardSetState.focusRequester,
        onTextChanged = {
            newCardSetState.updateTextFieldValue(it)  // update UI text field state
            vm.onNewCardSetTextChange(it.text) // update VM text state
        },
        onCreated = {
            vm.onCardSetAdded(it)
        }
    )
    is CardSetViewItem -> CardSetTitleView(
        item,
        onClick = { vm.onCardSetClicked(item) },
        onDeleted = { vm.onCardSetRemoved(item) }
    )
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}


@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun CardSetTitleView(
    cardSetViewItem: CardSetViewItem,
    onClick: () -> Unit,
    onDeleted: () -> Unit
) {
    DeletableCell(
        stateKey = cardSetViewItem.id,
        onClick,
        onDeleted
    ) {
        Row(
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.article_horizontalPadding),
                end = dimensionResource(id = R.dimen.article_horizontalPadding)
            )
        ) {
            Text(
                text = cardSetViewItem.name,
                modifier = Modifier.weight(1.0f, true),
                style = AppTypography.articleTitle
            )
            Text(
                text = cardSetViewItem.date,
                style = AppTypography.articleDate
            )
        }
    }
}

//@ExperimentalAnimationApi
//@ExperimentalMaterialApi
//@ExperimentalComposeUiApi
//@Preview
//@Composable
//fun CardSetsUIPreviewWithArticles() {
//    ComposeAppTheme {
//        CardSetsUI(
//            vm = CardSetsVM(
//                articles = Resource.Loaded(
//                    data = listOf(
//                        ArticleViewItem(1, "Article Name", "Today")
//                    )
//                )
//            )
//        )
//    }
//}

