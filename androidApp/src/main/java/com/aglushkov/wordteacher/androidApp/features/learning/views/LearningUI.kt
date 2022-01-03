package com.aglushkov.wordteacher.androidApp.features.learning.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordDefinitionView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordExampleView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordPartOfSpeechView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordSubHeaderView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordSynonymView
import com.aglushkov.wordteacher.androidApp.features.definitions.views.WordTitleView
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordDefinitionViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordExampleViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordPartOfSpeechViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSubHeaderViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordSynonymViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningUI(
    vm: LearningVM,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val viewItemsRes by vm.viewItems.collectAsState()
    val data = viewItemsRes.data()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.learning_title))
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
                    bottom = 300.dp
                )
            ) {
                items(data, key = { it.id }) { item ->
                    LearningViewItems(Modifier.animateItemPlacement(), item, vm)
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
fun LearningViewItems(
    modifier: Modifier,
    itemView: BaseViewItem<*>,
    vm: LearningVM,
) {
    when(val item = itemView) {
        is WordTitleViewItem -> WordTitleView(item, modifier)
        is WordPartOfSpeechViewItem -> WordPartOfSpeechView(item, modifier)
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
