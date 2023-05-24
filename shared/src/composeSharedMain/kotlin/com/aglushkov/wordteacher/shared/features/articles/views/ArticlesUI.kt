package com.aglushkov.wordteacher.shared.features.articles.views

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.CustomTopAppBar
import com.aglushkov.wordteacher.shared.general.views.DeletableCell
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.general.views.SearchView
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

@Composable
fun ArticlesUI(
    vm: ArticlesVM,
    modifier: Modifier = Modifier
) {
    val articles by vm.articles.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column{
            CustomTopAppBar {
                SearchView(searchText, onTextChanged = { searchText = it }) {
                    //vm.onWordSubmitted(searchText)
                }
            }

            val data = articles.data()

            if (data?.isNotEmpty() == true) {
                LazyColumn {
                    items(
                        data,
                        key = { it.id }
                    ) { item ->
                        ArticlesViewItem(item, vm)
                    }
                }
            } else {
                LoadingStatusView(
                    resource = articles,
                    loadingText = null,
                    errorText = vm.getErrorText(articles)?.localized(),
                    emptyText = stringResource(MR.strings.articles_no_articles)
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
                onClick = { vm.onCreateTextArticleClicked() },
                modifier = Modifier.padding(
                    LocalDimensWord.current.articleHorizontalPadding
                )
            ) {
                Icon(
                    painter = painterResource(MR.images.add_white_24),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ArticlesViewItem(
    item: BaseViewItem<*>,
    vm: ArticlesVM
) = when (item) {
    is ArticleViewItem -> ArticleTitleView(
        item,
        onClick = { vm.onArticleClicked(item) },
        onDeleted = { vm.onArticleRemoved(item) }
    )
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ArticleTitleView(
    articleViewItem: ArticleViewItem,
    onClick: () -> Unit,
    onDeleted: () -> Unit
) {
    DeletableCell(
        Modifier,
        stateKey = articleViewItem.id,
        onClick,
        onDeleted
    ) {
        ListItem(
            text = { Text(articleViewItem.name) },
            secondaryText = { Text(articleViewItem.date) }
        )
    }
}

// TODO: replace with coerceIn
fun Float.roundToMax(value: Float) = kotlin.math.min(this, value)
fun Float.roundToMin(value: Float) = kotlin.math.max(this, value)
fun Int.roundToMax(value: Int) = kotlin.math.min(this, value)
fun Int.roundToMin(value: Int) = kotlin.math.max(this, value)
