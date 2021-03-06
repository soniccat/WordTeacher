package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.general.item.BaseViewItem
import com.aglushkov.wordteacher.shared.model.Article

class ArticleViewItem(
    articleId: Long,
    val name: String,
    val date: String
): BaseViewItem<String>(name, Type, articleId) {
    companion object {
        const val Type = 200
    }

    override fun equalsByContent(other: BaseViewItem<*>): Boolean {
        other as ArticleViewItem
        return super.equalsByContent(other) && date == other.date
    }
}
