package com.aglushkov.wordteacher.shared.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString

@Composable
fun HtmlString.toAnnotatedString(
    linkColor: Color,
    onLinkClicked: ((url: String) -> Unit)? = null,
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        links.onEach { link ->
//            addStyle(SpanStyle(color = linkColor), it.start, it.end)
            addLink(
                LinkAnnotation.Clickable(
                    tag = HTML_STRING_TAG_LINK,
                    styles = TextLinkStyles(
                        style = SpanStyle(color = linkColor)
                    ),
                    linkInteractionListener = {
                        onLinkClicked?.invoke(link.href)
                    }
                ),
                link.start,
                link.end
            )
//            addStringAnnotation(HTML_STRING_TAG_LINK, it.href, it.start, it.end)
        }
    }
}

const val HTML_STRING_TAG_LINK = "link"
