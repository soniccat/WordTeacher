package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.general.resource.StringResourceWrapper
import org.jetbrains.compose.resources.StringResource

class WordSuggestDictEntryViewItem(
    word: String,
    val definition: String,
    val source: String,
): BaseViewItem<String>(word, Type) {
    companion object {
        const val Type = 1001
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                definition == (other as WordSuggestDictEntryViewItem).definition &&
                source == other.source
    }
}

class WordSuggestByTextViewItem(
    foundText: String,
    val wordIndex: Int,
    val defPairIndex: Int,
    val defEntryIndex: Int,
    val exampleIndex: Int,
    val source: String,
): BaseViewItem<String>(foundText, Type) {
    companion object {
        const val Type = 1002
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                wordIndex == (other as WordSuggestByTextViewItem).wordIndex &&
                defPairIndex == other.defPairIndex &&
                defEntryIndex == other.defEntryIndex &&
                exampleIndex == other.exampleIndex &&
                source == other.source
    }
}

class WordTextSearchHeaderViewItem(
    titleText: StringResourceWrapper,
    val showAllWordsActionText: StringResourceWrapper,
    val isTop: Boolean,
): BaseViewItem<StringResourceWrapper>(titleText, Type) {
    companion object {
        const val Type = 1003
    }
}

class WordCorrectionsHeaderViewItem(
    titleText: StringResourceWrapper,
    val isTop: Boolean,
): BaseViewItem<StringResourceWrapper>(titleText, Type) {
    companion object {
        const val Type = 1010
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                isTop == (other as WordCorrectionsHeaderViewItem).isTop
    }
}

class WordCorrectionsMisspellingDBFillProgress(
    val titleText: StringResourceWrapper,
    val isLoading: Boolean,
    val progress: Float
): BaseViewItem<StringResourceWrapper>(titleText, Type) {
    companion object {
        const val Type = 1011
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                isLoading == (other as WordCorrectionsMisspellingDBFillProgress).isLoading &&
                progress == other.progress
    }
}