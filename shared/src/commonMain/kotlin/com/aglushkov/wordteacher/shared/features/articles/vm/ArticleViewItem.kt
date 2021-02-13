package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article

class ArticleViewItem(
    val name: String,
    val date: Long
): BaseViewItem<String>(name, Type) {

    companion object {
        const val Type = 200
    }

    override fun equalsByIds(item: BaseViewItem<*>): Boolean {
        return item is ArticleViewItem && item.type == Type
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as ArticleViewItem
        return name == other.name && date == other.date
    }
}
