package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.LocalDimens

@Composable
fun LoadingViewItemUI() {
    val side = 30.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LocalDimens.current.contentPadding,
                end = LocalDimens.current.contentPadding,
            )
    ) {
        CircularProgressIndicator(modifier = Modifier.size(side, side).align(Alignment.Center))
    }
}