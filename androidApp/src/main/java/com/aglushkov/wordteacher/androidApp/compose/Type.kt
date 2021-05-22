package com.aglushkov.wordteacher.androidApp.compose

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aglushkov.wordteacher.androidApp.R

// Set of Material typography styles to start with
val typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)

class AppTypography {
    companion object {
        val wordDefinitionTitle = typography.h5
        val wordDefinitionProvidedBy: TextStyle
            @Composable get() {
                val color = colorResource(id = R.color.word_provided_by)
                return remember(color) { typography.body2.copy(
                    color = color
                )}
            }
        val wordDefinitionTranscripton = typography.body2
        val wordDefinitionPartOfSpeech: TextStyle
            @Composable get() {
                val color = colorResource(id = R.color.word_partOfSpeech)
                return remember(color) { typography.subtitle2.copy(
                    color = color
                )}
            }
        val wordDefinition = typography.body2
        val wordDefinitionSubHeader: TextStyle
            @Composable get() {
                val color = colorResource(id = R.color.word_subheader)
                return remember(color) { typography.subtitle2.copy(
                    color = color
                )}
            }
    }
}
