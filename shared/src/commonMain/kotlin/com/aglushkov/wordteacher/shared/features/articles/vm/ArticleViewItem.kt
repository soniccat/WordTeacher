package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ArticleViewItem(
    override val id: Long,
    val name: String,
    val date: String,
    override val type: Int = 200,
): BaseViewItem<String> {
    override val items: ImmutableList<String> = persistentListOf(name)

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as ArticleViewItem
        return super.equalsByContent(other) && date == other.date
    }

    override fun copyWithId(id: Long) = this.copy(id = id)
}
