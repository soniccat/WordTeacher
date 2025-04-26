package com.aglushkov.wordteacher.shared.features.articles.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.LocalDimensWord
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.views.CustomAnnotatedTextListItem
import com.aglushkov.wordteacher.shared.general.views.DeletableCell
import com.aglushkov.wordteacher.shared.general.views.LoadingStatusView
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.compose.localized

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
//            CustomTopAppBar {
//                SearchView(Modifier, searchText, onTextChanged = { searchText = it }) {
//                    //vm.onWordSubmitted(searchText)
//                }
//            }
            TopAppBar(
                title = { Text(text = stringResource(MR.strings.articles_title)) }
            )

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

@Composable
fun ArticleTitleView(
    articleViewItem: ArticleViewItem,
    onClick: () -> Unit,
    onDeleted: () -> Unit
) {
    DeletableCell(
        Modifier,
        stateKey = articleViewItem.id,
        enabled = true,
        onClick,
        onDeleted
    ) {
        ArticleTitleView(Modifier, articleViewItem)
    }
}

@Composable
fun ArticleTitleView(
    modifier: Modifier,
    articleViewItem: ArticleViewItem
) {
    CustomAnnotatedTextListItem(
        modifier = modifier.alpha(
            if (articleViewItem.isRead) 0.5f else 1.0f
        ),
        title = articleViewItem.name,
    )
}
