package com.aglushkov.wordteacher.desktopapp.features.definitions.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.features.definitions.vm.Indent
import com.aglushkov.wordteacher.shared.general.LocalDimens

@Composable
fun Indent.toDp(): Dp {
    return when (this) {
        Indent.SMALL -> LocalDimens.current.indentSmall
        Indent.NONE -> 0.0f.dp
    }
}
