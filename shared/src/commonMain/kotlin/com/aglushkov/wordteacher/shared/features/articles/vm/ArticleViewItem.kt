package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article

class ArticleViewItem(
    val articleId: Long,
    val name: String,
    val date: String,
    val isRead: Boolean,
): BaseViewItem<Long>(articleId, Type) {
    companion object {
        const val Type = 200
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as ArticleViewItem
        return super.equalsByContent(other) && date == other.date &&
                isRead == other.isRead
    }
}
