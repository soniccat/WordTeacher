package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens

@Composable
fun ListSectionCell(
    text: String,
    modifier: Modifier = Modifier,
    actionBlock: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f))
    ) {
        Text(
            text,
            modifier = Modifier
                .weight(1.0f)
                .padding(
                    start = LocalDimens.current.contentPadding,
                    end = LocalDimens.current.contentPadding
                ),
            style = LocalAppTypography.current.wordDefinitionSubHeader
        )
        actionBlock.invoke(this)
    }
}