package com.aglushkov.wordteacher.shared.general

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DimensWord(
    val dimens: Dimens,
    val wordHorizontalPadding: Dp = dimens.contentPadding,
    val wordDividerTopMargin: Dp = dimens.contentPadding,
    val wordDividerBottomMargin: Dp = 12.dp,
    val wordPartOfSpeechTopMargin: Dp = 20.dp,
    val wordHeaderTopMargin: Dp = 12.dp,
    val wordSubHeaderTopMargin: Dp = 2.dp,

    val wordDividerHeight: Dp = 1.dp,
    val wordProvidedByMaxWidth: Dp = 150.dp,

    val articleHorizontalPadding: Dp = dimens.definitionsSearchHorizontalMargin,
    val articleDividerTopMargin: Dp = dimens.contentPadding,

    val noteHorizontalPadding: Dp = dimens.definitionsSearchHorizontalMargin,
    val noteVerticalPadding: Dp = dimens.definitionsSearchHorizontalMargin,
    val noteDividerTopMargin: Dp = dimens.contentPadding,

    val learningHorizontalPadding: Dp = dimens.contentPadding,
)

val LocalDimensWord = staticCompositionLocalOf { DimensWord(Dimens()) }
