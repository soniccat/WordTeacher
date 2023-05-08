package com.aglushkov.wordteacher.desktopapp.general.views.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.getImageByFileName

// TODO: replace with material one
@Composable
fun Chip(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean = false,
    colors: ChipColors? = null,
    isCloseIconVisible: Boolean = false,
    closeBlock: (() -> Unit)? = null,
    clickBlock: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .background(
                color = when {
                    isChecked -> colors?.checkedBgColor ?: MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
                    else -> colors?.bgColor ?: MaterialTheme.colors.onSurface.copy(alpha = 0.10f)
                },
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable {
                clickBlock?.invoke()
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChecked) {
            Icon(
                painter = painterResource(MR.images.check.filePath),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(
                color = colors?.contentColor ?: MaterialTheme.colors.onSurface,
                fontSize = 14.sp
            ),
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 7.dp),
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1
        )
        if (isCloseIconVisible && closeBlock != null) {
            Icon(
                painter = painterResource(MR.images.close_18.filePath),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clickable {
                        closeBlock()
                    },
                tint = colors?.closeIconTint ?: MaterialTheme.colors.onSurface.copy(alpha = 0.87f)
            )
        }
    }
}

data class ChipColors(
    val contentColor: Color? = null,
    val bgColor: Color? = null,
    val checkedBgColor: Color? = bgColor,
    val closeIconTint: Color? = contentColor
)
//
//@Preview
//@Composable
//fun ChipPreview() {
//    ComposeAppTheme {
//        Column(
//        ) {
//            Chip(
//                modifier = Modifier.padding(4.dp),
//                text = "By Source",
//                isChecked = true,
//                colors = null,
//                isCloseIconVisible = true,
//                closeBlock = {},
//                clickBlock = {}
//            )
//            Chip(
//                modifier = Modifier.padding(4.dp),
//                text = "By Source",
//                isChecked = false,
//                colors = null,
//                isCloseIconVisible = true,
//                closeBlock = {},
//                clickBlock = {}
//            )
//            Chip(
//                modifier = Modifier.padding(4.dp),
//                text = "Add Filter",
//                isChecked = false,
//                colors = ChipColors(
//                    contentColor = MaterialTheme.colors.onSecondary,
//                    bgColor = MaterialTheme.colors.secondary
//                ),
//                isCloseIconVisible = false,
//                closeBlock = {},
//                clickBlock = {}
//            )
//            Chip(
//                modifier = Modifier.padding(4.dp),
//                text = "Add Filter",
//                isChecked = false,
//                colors = ChipColors(
//                    contentColor = MaterialTheme.colors.onSecondary,
//                    bgColor = MaterialTheme.colors.secondary
//                ),
//                isCloseIconVisible = true,
//                closeBlock = {},
//                clickBlock = {}
//            )
//        }
//    }
//}
