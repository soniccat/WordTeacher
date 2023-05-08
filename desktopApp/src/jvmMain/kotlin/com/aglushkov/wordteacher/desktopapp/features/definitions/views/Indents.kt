package com.aglushkov.wordteacher.desktopapp.features.definitions.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent

@Composable
fun Indent.toDp(): Dp {
    val px = when (this) {
        Indent.SMALL -> 15.0f //LocalContext.current.resources.getDimensionPixelOffset(R.dimen.indent_small)
        Indent.NONE -> 0.0f
    }
    return Dp(px)
}

fun Indent.toPx() = when (this) {
    Indent.SMALL -> 15.0f // res.getDimensionPixelOffset(R.dimen.indent_small)
    Indent.NONE -> 0
}
