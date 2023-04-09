package com.aglushkov.wordteacher.shared.features.definitions.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class WordSuggestViewItem(
    override val id: Long = 0L,
    override val type: Int = 1001,
    word: String,
    override val items: ImmutableList<String> = persistentListOf(word),
    val definition: String,
    val source: String,
): BaseViewItem<String> {
    override fun copyWithId(id: Long): BaseViewItem<String> = WordSuggestViewItem(
        id = id,
        type = type,
        word = items.first(),
        definition = definition,
        source = source
    )

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        return super.equalsByContent(other) &&
                definition == (other as WordSuggestViewItem).definition &&
                source == other.source
    }
}