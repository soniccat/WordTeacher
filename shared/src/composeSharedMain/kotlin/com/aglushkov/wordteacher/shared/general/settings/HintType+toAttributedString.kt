package com.aglushkov.wordteacher.shared.general.settings

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
import androidx.compose.ui.unit.em
import com.aglushkov.wordteacher.shared.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource

private const val HintIntroduction_Close = "Hint_Close"
private const val DashboardArticles_Download = "DashboardArticles_Download"
private const val DashboardCardsets_Add = "DashboardCardsets_Add"
private const val DashboardCardsets_Learn = "DashboardCardsets_Learn"
private const val Article_Menu = "Article_Menu"

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
    },
    DashboardCardsets_Add to
    InlineTextContent(inlineTextPlaceholder) {
        TextIcon(
            imageResource = MR.images.add_white_24,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.87f),
        )
    },
    DashboardCardsets_Learn to
    InlineTextContent(inlineTextPlaceholder) {
        TextIcon(
            imageResource = MR.images.start_learning_rounded_24,
            tint = MaterialTheme.colors.secondary,
        )
    },
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
        HintType.Introduction -> buildAnnotatedString {
            append("Hi, I'm glad to see you here. I'm a hint. After reading the text, tap ")
            appendInlineContent(HintIntroduction_Close, "[Close]")
            append(" to hide me.")
        }
        HintType.DashboardArticles -> buildAnnotatedString {
            append("To start learning new words, pick ones you're interested in and add them to a card set. This screen offers two types of word sources.")
            appendEndOfParagraph()
            append("The first is news. Choose an article you want to read and tap ")
            appendInlineContent(DashboardArticles_Download, "[Download]")
            append(" to save the article.")
            appendEndOfParagraph()
            append("To preview the content first, tap the headline. You’ll find ")
            appendInlineContent(DashboardArticles_Download, "[Download]")
            append(" in the top bar on the next screen too.")
        }
        HintType.DashboardCardSets -> buildAnnotatedString {
            append("The second word source is pre-made card sets. Tap a card set to preview it. If you like it, tap ")
            appendInlineContent(DashboardCardsets_Add, "[Plus]")
            append(" at the bottom of the next screen to save the card set.")
        }
        HintType.DashboardUsersArticles -> buildAnnotatedString {
            append("Below are your recently saved articles, so you can quickly return to your current reading.")
        }
        HintType.DashboardUsersCardSets -> buildAnnotatedString {
            append("Below are the card sets you've chosen to learn. Mastering them takes time, so tap ")
            appendInlineContent(DashboardCardsets_Learn, "[Plus]")
            append(" regularly to continue your progress.")
        }
        HintType.Articles -> buildAnnotatedString {
            append("Here you'll find your saved articles, with the most recent ones at the top. Press ")
            appendInlineContent(DashboardCardsets_Add, "[Plus]")
            append(" to add a new article by pasting text from your clipboard.")
            appendEndOfParagraph()
            append("You can also share articles directly to the app from your browser for quick importing.")
        }
        HintType.AddArticle -> buildAnnotatedString {
            append("Try sharing an article from your browser to the app for automatic text extraction.")
        }
        HintType.Article -> buildAnnotatedString {
            append("Tap any word or highlighted phrase to get its definition. Add unfamiliar words to a card set and start learning them.")
            appendEndOfParagraph()
            append("To get a word's definition within a highlighted phrase, press and hold it for 2 seconds.")
            appendEndOfParagraph()
            append("Tap ")
            appendInlineContent(Article_Menu, "[Menu]")
            append("to adjust text highlighting settings.")
        }
        HintType.CardSets -> buildAnnotatedString {
            append("All your card sets are displayed here with each set's progress shown on the right. This progress is calculated from your completion of individual cards within the set.")
            appendEndOfParagraph()
            append("Discover card sets created by other users - search by word, phrase, or part of a title to import them")
            appendEndOfParagraph()
            append("Sign In from the Settings screen to sync your card sets and their progress between devices.")
        }
    }
}

private fun AnnotatedString.Builder.appendEndOfParagraph() {
    appendLine()
    addStyle(ParagraphStyle(lineHeight = 0.02.em), length - 1, length)
}
