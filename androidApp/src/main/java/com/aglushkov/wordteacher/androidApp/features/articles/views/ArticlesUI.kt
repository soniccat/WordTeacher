package com.aglushkov.wordteacher.androidApp.features.articles.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM

@Composable
fun ArticlesUI(
    vm: ArticlesVM
) {
    Box(
        modifier = Modifier
            .background(color = Color.Red)
            .fillMaxSize()
    ) {

    }
}