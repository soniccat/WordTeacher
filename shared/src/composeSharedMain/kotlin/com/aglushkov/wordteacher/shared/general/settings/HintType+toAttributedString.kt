package com.aglushkov.wordteacher.shared.general.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.aglushkov.wordteacher.shared.general.LocalDimens
import com.aglushkov.wordteacher.shared.general.views.DownloadForOfflineButton
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource

private const val HintIntroduction_Close = "Hint_Close"
private const val DashboardArticles_Download = "DashboardArticles_Download"
private val inlineTextPlaceholder = Placeholder(
    width = 1.2.em,
    height = 1.2.em,
    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
)

val HintInlineContent = mapOf(
    HintIntroduction_Close to
    InlineTextContent(inlineTextPlaceholder) {
        TextIcon(
            imageResource = MR.images.close_18,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.87f),
        )
    },
    DashboardArticles_Download to
    InlineTextContent(inlineTextPlaceholder) {
        TextIcon(
            imageResource = MR.images.download_for_offline,
            tint = MaterialTheme.colors.secondary,
        )
    }
)

@Composable
fun TextIcon(
    imageResource: ImageResource,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        painterResource(imageResource),
        null,
        modifier = Modifier.clip(CircleShape).then(modifier),
        tint = tint
    )
}

fun HintType.toAttributedString(): AnnotatedString {
    return when (this) {
        HintType.HintIntroduction -> buildAnnotatedString {
            append("Hi, I'm glad to see you here. I'm a hint. After reading the text, tap ")
            appendInlineContent(HintIntroduction_Close, "[Close]")
            append(" not to see me again.")
        }
        HintType.DashboardArticles -> buildAnnotatedString {
            append("To start leaning new words you need to pick ones you're interested in and put them in a card set. This screen offers you two types of word sources.")
            appendLine()
            addStyle(ParagraphStyle(lineHeight = 0.02.em), length - 1, length)
            append("The first is news. Choose an article you want to read and press ")
            appendInlineContent(DashboardArticles_Download, "[Download]")
            append(" to download it. Also, If you prefer to preview the content, tap on the headline and icon in the top bar. Then open the article, new hints are waiting you there.")
        }
        HintType.DashboardCardSets -> buildAnnotatedString {
        }
        HintType.DashboardUsersArticles -> buildAnnotatedString {
        }
        HintType.DashboardUsersCardSets -> buildAnnotatedString {
        }
    }
}
