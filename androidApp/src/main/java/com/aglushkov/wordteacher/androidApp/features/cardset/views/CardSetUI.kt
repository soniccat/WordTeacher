package com.aglushkov.wordteacher.androidApp.features.cardset.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.*
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.features.cardset.vm.CreateCardViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

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
            LazyColumn {
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
        is WordDividerViewItem -> WordDividerView(modifier)
        is WordTitleViewItem -> WordTitleView(item, modifier)
        is WordTranscriptionViewItem -> WordTranscriptionView(item, modifier)
        is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item, modifier)
        is WordDefinitionViewItem -> WordDefinitionView(item, modifier)
        is WordSubHeaderViewItem -> WordSubHeaderView(item, modifier)
        is WordSynonymViewItem -> WordSynonymView(item, modifier)
        is WordExampleViewItem -> WordExampleView(item, modifier)
        is CreateCardViewItem -> CardView(
            item,
            onClicked = {
                vm.onCardSetCreatePressed()
            }
        )
        else -> {
            Text(
                text = "unknown item $item",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CardView(
    viewItem: CreateCardViewItem,
    onClicked: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
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
