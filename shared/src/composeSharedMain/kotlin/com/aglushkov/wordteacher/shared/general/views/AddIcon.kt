package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.LocalDimens
import wordteacher.shared.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import wordteacher.shared.generated.resources.plus_medium
import wordteacher.shared.generated.resources.plus_small

enum class AddIconStyle{
    Small,
    Medium
}

@Composable
fun AddIcon(
    modifier: Modifier = Modifier,
    style: AddIconStyle = AddIconStyle.Small,
    contentPadding: PaddingValues = PaddingValues(
        if (style == AddIconStyle.Small) {
            4.dp
        } else {
            LocalDimens.current.halfOfContentPadding
        }
    ),
    onClicked: () -> Unit
) {
    Icon(
        painter = painterResource(
            if (style == AddIconStyle.Small) {
                Res.drawable.plus_small
            } else {
                Res.drawable.plus_medium
            }
        ),
        contentDescription = null,
        modifier = modifier
            .clip(CircleShape)
            .clickable {
                onClicked()
            }
            .padding(contentPadding),
        tint = MaterialTheme.colors.secondary
    )
}