package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import dev.icerock.moko.resources.desc.StringDesc

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
    titleText: StringDesc,
    val showAllWordsActionText: StringDesc,
    val isTop: Boolean,
): BaseViewItem<StringDesc>(titleText, Type) {
    companion object {
        const val Type = 1003
    }
}

class WordCorrectionsHeaderViewItem(
    titleText: StringDesc,
    val isTop: Boolean,
): BaseViewItem<StringDesc>(titleText, Type) {
    companion object {
        const val Type = 1010
    }
}

class WordCorrectionsMisspellingDBFillProgress(
    val titleText: StringDesc,
    val isLoading: Boolean,
    val progress: Float
): BaseViewItem<StringDesc>(titleText, Type) {
    companion object {
        const val Type = 1011
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                isLoading == (other as WordCorrectionsMisspellingDBFillProgress).isLoading &&
                progress == other.progress
    }
}