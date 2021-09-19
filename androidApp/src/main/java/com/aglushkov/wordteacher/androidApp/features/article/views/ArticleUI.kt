package com.aglushkov.wordteacher.androidApp.features.article.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM

@Composable
fun ArticleUI(
    vm: ArticleVM
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Cyan)
    ) {

    }
}