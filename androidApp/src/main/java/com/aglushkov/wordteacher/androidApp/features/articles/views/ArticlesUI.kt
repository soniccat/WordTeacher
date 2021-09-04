package com.aglushkov.wordteacher.androidApp.features.articles.views

import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.compose.AppTypography
import com.aglushkov.wordteacher.androidApp.compose.ComposeAppTheme
import com.aglushkov.wordteacher.androidApp.general.extensions.resolveString
import com.aglushkov.wordteacher.androidApp.general.views.compose.CustomTopAppBar
import com.aglushkov.wordteacher.androidApp.general.views.compose.LoadingStatusView
import com.aglushkov.wordteacher.androidApp.general.views.compose.SearchView
import com.aglushkov.wordteacher.androidApp.general.views.compose.pxToDp
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets

@ExperimentalComposeUiApi
@Composable
fun ArticlesUI(
    vm: ArticlesVM,
    modifier: Modifier = Modifier
) {
    val articles by vm.articles.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isAddDialogVisible by remember { mutableStateOf(false)}

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column{
            CustomTopAppBar {
                SearchView(searchText, { searchText = it }) {
                    //vm.onWordSubmitted(searchText)
                    isAddDialogVisible = true
                }
            }

            val data = articles.data()

            if (data?.isNotEmpty() == true) {
                LazyColumn {
                    items(data) { item ->
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
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }

        if (isAddDialogVisible) {
            //CompositionLocalProvider(LocalView provides LocalView.current) {
                CustomDialog(
                    onDismissRequest = {
                        isAddDialogVisible = false
                    }
                )
            //}
        }
    }
}

@ExperimentalComposeUiApi
@Composable
private fun CustomDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val p = LocalView.current
        val p2 = p.parent
        val windowProvider = p2 as DialogWindowProvider
        val window = windowProvider.window
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        window.decorView.requestLayout()

//        var topOffset by remember { mutableStateOf(0) }
//        var bottomOffset by remember { mutableStateOf(0) }
//        var leftOffset by remember { mutableStateOf(0) }
//        var rightOffset by remember { mutableStateOf(0) }
//        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
//            Log.d("hminsets", "" + insets.stableInsetBottom +
//                    " " + insets.systemWindowInsetBottom +
//                    " " + insets.stableInsetTop +
//                    " " + insets.systemWindowInsetTop +
//                    " " + insets.stableInsetLeft +
//                    " " + insets.systemWindowInsetLeft
//            )
//            topOffset = insets.systemWindowInsetTop
//            bottomOffset = insets.systemWindowInsetBottom
//            leftOffset = insets.systemWindowInsetLeft
//            rightOffset = insets.systemWindowInsetRight
//
//            window.decorView.requestLayout()
//
//            insets.consumeSystemWindowInsets()
//        }

        ProvideWindowInsets {
            val insets = LocalWindowInsets.current

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = insets.systemBars.left.pxToDp(),
                        end = insets.systemBars.right.pxToDp(),
                        top = insets.systemBars.top.pxToDp(),
                        bottom = insets.systemBars.bottom.pxToDp()
                    ),
                color = MaterialTheme.colors.background
            ) {
                Column {
                    TextField(
                        value = "hmm",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text("that's me 2")
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
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