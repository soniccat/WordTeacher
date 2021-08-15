package com.aglushkov.wordteacher.androidApp.features.articles.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.WordTitleViewItem
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

@Composable
fun ArticlesUI(
    vm: ArticlesVM,
    modifier: Modifier = Modifier
) {
    val defs = vm.articles.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
    ) {
        CustomTopAppBar {
            SearchView(searchText, { searchText = it }) {
                //vm.onWordSubmitted(searchText)
            }
        }

        val res = defs.value
        val data = res.data()

        if (data?.isNotEmpty() == true) {
            LazyColumn {
                items(data) { item ->
                    articlesViewItem(item, vm)
                }
            }
        } else {
            LoadingStatusView(
                resource = res,
                loadingText = null,
                errorText = vm.getErrorText(res)?.resolveString(),
                emptyText = LocalContext.current.getString(R.string.articles_no_articles)
            ) {
                vm.onTryAgainClicked()
            }
        }
    }
}

@Composable
private fun articlesViewItem(
    item: BaseViewItem<*>,
    vm: ArticlesVM
) = when (item) {
    is ArticleViewItem -> ArticleTitleView(item)
    else -> {
        Text(
            text = "unknown item $item"
        )
    }
}


@Composable
private fun ArticleTitleView(
    articleViewItem: ArticleViewItem
) {
    Column(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.article_horizontalPadding),
            end = dimensionResource(id = R.dimen.article_horizontalPadding)
        )
    ) {
        Text(
            text = articleViewItem.name,
            modifier = Modifier
                .weight(1.0f, true),
            style = AppTypography.articleTitle
        )
        Text(
            text = articleViewItem.date,
            style = AppTypography.articleDate
        )
    }
}