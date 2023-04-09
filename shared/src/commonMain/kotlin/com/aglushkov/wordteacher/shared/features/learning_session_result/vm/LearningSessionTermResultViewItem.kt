package com.aglushkov.wordteacher.shared.features.learning_session_result.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LearningSessionTermResultViewItem(
    override val id: Long,
    val term: String,
    var newProgress: Float,
    var isRight: Boolean,
    override val type: Int = 901,
): BaseViewItem<String> {
    override val items: ImmutableList<String> = persistentListOf(term)

    override fun copyWithId(id: Long): BaseViewItem<String> = this.copy(id = id)
}
