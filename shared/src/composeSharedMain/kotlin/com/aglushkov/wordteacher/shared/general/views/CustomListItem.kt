package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.shared.general.LocalDimens

@Composable
fun CustomListItem(
    modifier: Modifier = Modifier.padding(vertical = LocalDimens.current.contentPadding),
    trailing: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.weight(1f)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.CenterStart
        ) { content() }
        if (trailing != null) {
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = LocalDimens.current.contentPadding)
            ) { trailing() }
        }
    }
}
