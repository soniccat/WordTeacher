package com.aglushkov.wordteacher.android_app.features.definitions.views

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent

@Composable
fun Indent.toDp(): Dp {
    val px = when (this) {
        Indent.SMALL -> LocalContext.current.resources.getDimensionPixelOffset(R.dimen.indent_small)
        Indent.NONE -> 0
    }
    return Dp(px / LocalDensity.current.density)
}

fun Indent.toPx(res: Resources) = when (this) {
    Indent.SMALL -> res.getDimensionPixelOffset(R.dimen.indent_small)
    Indent.NONE -> 0
}
