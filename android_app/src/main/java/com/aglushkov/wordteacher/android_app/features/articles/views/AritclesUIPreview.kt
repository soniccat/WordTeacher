package com.aglushkov.wordteacher.android_app.features.articles.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import com.aglushkov.wordteacher.android_app.compose.ComposeAppTheme
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesUI
import com.aglushkov.wordteacher.shared.features.articles.views.ArticlesVMPreview
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticleViewItem
import com.aglushkov.wordteacher.shared.general.resource.Resource

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
                        ArticleViewItem(1, "Article Name", "Today", false)
                    )
                )
            )
        )
    }
}