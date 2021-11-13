package com.aglushkov.wordteacher.androidApp.features.articles.views

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
import androidx.compose.ui.tooling.preview.Preview
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.DeletableCell
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
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
                SearchView(searchText, { searchText = it }) {
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
                    errorText = vm.getErrorText(articles)?.resolveString(),
                    emptyText = LocalContext.current.getString(R.string.articles_no_articles)
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
                    dimensionResource(id = R.dimen.article_horizontalPadding)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_white_24dp),
                    contentDescription = null
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
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


@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun ArticleTitleView(
    articleViewItem: ArticleViewItem,
    onClick: () -> Unit,
    onDeleted: () -> Unit
) {
    DeletableCell(
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
                text = articleViewItem.name,
                modifier = Modifier.weight(1.0f, true),
                style = AppTypography.articleTitle
            )
            Text(
                text = articleViewItem.date,
                style = AppTypography.articleDate
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Preview
@Composable
fun ArticlesUIPreviewWithArticles() {
    ComposeAppTheme {
        ArticlesUI(
            vm = ArticlesVMPreview(
                articles = Resource.Loaded(
                    data = listOf(
                        ArticleViewItem(1, "Article Name", "Today")
                    )
                )
            )
        )
    }
}

fun Float.roundToMax(value: Float) = kotlin.math.min(this, value)
fun Float.roundToMin(value: Float) = kotlin.math.max(this, value)
fun Int.roundToMax(value: Int) = kotlin.math.min(this, value)
fun Int.roundToMin(value: Int) = kotlin.math.max(this, value)
