package com.aglushkov.wordteacher.androidApp.features.articles.views

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
                        articlesViewItem(item, vm)
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

@ExperimentalMaterialApi
@Composable
private fun articlesViewItem(
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


@ExperimentalMaterialApi
@Composable
private fun ArticleTitleView(
    articleViewItem: ArticleViewItem,
    onClick: () -> Unit,
    onDeleted: () -> Unit
) {
    var isSwipedAway by remember {
        mutableStateOf(false)
    }
    val dismissState = rememberDismissState(
//        confirmStateChange = {
//            //Log.d("test", "state ${it}")
//            if (it == DismissValue.DismissedToStart) {
//                //onDeleted()
//            }
//            true
//        }
    )

    val heightMultiplier: Float by animateFloatAsState(if (isSwipedAway) 0.0f else 1.0f)

    SwipeToDismiss(
        state = dismissState,
        modifier = Modifier
            .clickable {
            onClick()
        },
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Box(
                Modifier.fillMaxSize()
            ) {
                Layout(
                    content = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1.0f, matchHeightConstraintsFirst = true)
                                .background(androidx.compose.ui.graphics.Color.Red)
                                .padding(horizontal = 10.dp)
                        )
                    }
                ) { measurables, constraints ->
                    val resultConstraints = constraints.copy(maxHeight = (constraints.maxHeight * heightMultiplier).toInt())
                    val text = measurables[0].measure(resultConstraints)
                    val resultFraction = when (dismissState.progress.to) {
                        DismissValue.DismissedToStart -> dismissState.progress.fraction * 2.0f
                        else -> 0f
                    }.roundToMax(1.0f)

                    layout(resultConstraints.maxWidth, resultConstraints.maxHeight) {
                        text.placeRelative(
                            x = resultConstraints.maxWidth - (resultConstraints.maxHeight * resultFraction).toInt(),
                            y = 0
                        )
                    }
                }
            }
        }
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

    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.progress }
            .distinctUntilChanged()
            .collect {
                if (it.to == DismissValue.DismissedToStart && it.fraction == 1.0f) {
                    Log.d("test", "state ${dismissState.progress}")
                    isSwipedAway = true
                    //onDeleted()
                }
            }
    }
}

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