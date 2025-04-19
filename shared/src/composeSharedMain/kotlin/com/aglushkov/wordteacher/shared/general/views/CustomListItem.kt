package com.aglushkov.wordteacher.shared.general.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.shared.general.HTML_STRING_TAG_LINK
import com.aglushkov.wordteacher.shared.general.HtmlString
import com.aglushkov.wordteacher.shared.general.LocalAppTypography
import com.aglushkov.wordteacher.shared.general.LocalDimens
import java.text.AttributedString

@Composable
fun CustomTextListItem(
    modifier: Modifier = Modifier,
    trailing: @Composable (BoxScope.() -> Unit)? = null,
    title: String,
    subtitle: String? = null
) {
    CustomListItem(
        modifier = modifier,
        trailing = trailing,
        secondaryContent = subtitle?.let {
            {
                Text(
                    text = it,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalAppTypography.current.listItemSubtitle,
                )
            }
        }
    ) {
        Text(
            text = title,
            style = LocalAppTypography.current.listItemTitle
        )
    }
}

@Composable
fun CustomTextListItem(
    modifier: Modifier = Modifier,
    trailing: @Composable (BoxScope.() -> Unit)? = null,
    title: String,
    subtitle: AnnotatedString? = null,
) {
    CustomListItem(
        modifier = modifier,
        trailing = trailing,
        secondaryContent = subtitle?.let {
            {
                Text(
                    text = it,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalAppTypography.current.listItemSubtitle,
                )
            }
        }
    ) {
        Text(
            text = title,
            style = LocalAppTypography.current.listItemTitle,
        )
    }
}

@Composable
fun CustomListItem(
    modifier: Modifier = Modifier,
    trailing: @Composable (BoxScope.() -> Unit)? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = LocalDimens.current.contentPadding,
        top = LocalDimens.current.halfOfContentPadding,
        end = if (trailing == null) {
            LocalDimens.current.contentPadding
        } else {
            LocalDimens.current.halfOfContentPadding
        },
        bottom = LocalDimens.current.halfOfContentPadding
    ),
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(contentPadding),
    ) {
        Box(
            Modifier.weight(1f)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.CenterStart
        ) {
            if (secondaryContent == null) {
                content()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    content()
                    secondaryContent()
                }
            }
        }
        if (trailing != null) {
            Box(
                Modifier.align(Alignment.Top)
            ) { trailing() }
        }
    }
}
