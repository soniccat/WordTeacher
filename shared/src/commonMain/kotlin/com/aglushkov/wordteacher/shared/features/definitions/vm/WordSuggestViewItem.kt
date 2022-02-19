package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem

class WordSuggestViewItem(
    word: String,
    val definition: String,
    val source: String,
): BaseViewItem<String>(word, Type) {
    companion object {
        const val Type = 1001
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                definition == (other as WordSuggestViewItem).definition &&
                source == other.source
    }
}