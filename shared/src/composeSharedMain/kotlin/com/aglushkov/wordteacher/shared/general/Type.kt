package com.aglushkov.wordteacher.shared.general

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.compose.colorResource
import com.aglushkov.wordteacher.shared.res.MR

// Set of Material typography styles to start with
val materialTypography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)

data class AppTypography(
    var wordDefinitionTitle: TextStyle = materialTypography.h5,
    var wordDefinitionTranscripton: TextStyle = materialTypography.body2,
    var wordDefinitionProvidedBy: TextStyle = TextStyle.Default,
    var wordDefinitionPartOfSpeech: TextStyle = TextStyle.Default,

    var wordDefinition: TextStyle = materialTypography.body1,
    var wordSynonym: TextStyle = materialTypography.body2,
    var wordExample: TextStyle = materialTypography.body2,
    var wordDefinitionSubHeader: TextStyle = materialTypography.subtitle2,

    var articleTitle: TextStyle = wordDefinitionTitle,
    var articleDate: TextStyle = TextStyle.Default,
    var articleText: TextStyle = materialTypography.body1,
    var articleSideSheetSection: TextStyle = materialTypography.h5,
    var articleSideSheetItem: TextStyle = materialTypography.body1,

    var noteText: TextStyle = materialTypography.body1,
    var noteDate: TextStyle = TextStyle.Default,
    var notePlaceholder: TextStyle = TextStyle.Default,

    var learningSessionTerm: TextStyle = wordDefinitionTitle,
    var learningSessionProgress: TextStyle = wordDefinitionTitle,

    var dictConfigTitle: TextStyle = materialTypography.h5,
    var dictParamText: TextStyle = materialTypography.body2,

    var settingsTitle: TextStyle = materialTypography.h5,
) {
    @Composable
    fun initStylesFromResources(colors: Colors): AppTypography {
        wordDefinitionProvidedBy = materialTypography.body2.copy(color = colorResource(MR.colors.word_provided_by))
        wordDefinitionPartOfSpeech = materialTypography.subtitle2.copy(color = colorResource(MR.colors.word_partOfSpeech))
        articleDate = wordDefinitionProvidedBy
        noteDate = wordDefinitionProvidedBy
        notePlaceholder = noteText.copy(color = colors.secondary)
        return this
    }
}

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }