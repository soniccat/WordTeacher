package com.aglushkov.wordteacher.android_app.general.views.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.android_app.R

@Composable
fun AddIcon(
    onClicked: () -> Unit
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_plus_small),
        contentDescription = null,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClicked()
            }
            .padding(4.dp),
        tint = MaterialTheme.colors.secondary
    )
}