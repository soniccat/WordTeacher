package com.aglushkov.wordteacher.shared.general

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.compose.colorResource
import com.aglushkov.wordteacher.shared.res.MR

// Set of Material typography styles to start with
val typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)

data class AppTypography(
    var wordDefinitionTitle: TextStyle = typography.h5,
    var wordDefinitionTranscripton: TextStyle = typography.body2,

    var wordDefinition: TextStyle = typography.body1,
    var wordSynonym: TextStyle = typography.body2,
    var wordExample: TextStyle = typography.body2,
    var wordDefinitionSubHeader: TextStyle = typography.subtitle2,/*: TextStyle
        @Composable get() {
            val color = colorResource(id = R.color.word_subheader)
            return remember(color) { typography.subtitle2.copy(
                color = color
            )}
        }*/

    var articleTitle: TextStyle = wordDefinitionTitle,
    var articleSideSheetSection: TextStyle = typography.h5,
    var articleSideSheetItem: TextStyle = typography.body1,

    var noteText: TextStyle = typography.body1,

    var learningSessionTerm: TextStyle = wordDefinitionTitle,
    var learningSessionProgress: TextStyle = wordDefinitionTitle,
) {
    val wordDefinitionProvidedBy: TextStyle
        @Composable
        get() {
            val color = colorResource(MR.colors.word_provided_by)
            return remember(color) { typography.body2.copy(
                color = color
            )}
        }
    val wordDefinitionPartOfSpeech: TextStyle
        @Composable
        get() {
            val color = colorResource(MR.colors.word_partOfSpeech)
            return remember(color) { typography.subtitle2.copy(
                color = color
            )}
        }
    val articleDate: TextStyle
        @Composable
        get() {
            return wordDefinitionProvidedBy
        }
    val articleText: TextStyle
        @Composable
        get() {
            return typography.body1
        }
    val noteDate: TextStyle
        @Composable
        get() {
            return wordDefinitionProvidedBy
        }
    val notePlaceholder: TextStyle
        @Composable get() {
            return noteText.copy(
                color = MaterialTheme.colors.secondary
            )
        }
}

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }