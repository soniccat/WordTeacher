package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

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
