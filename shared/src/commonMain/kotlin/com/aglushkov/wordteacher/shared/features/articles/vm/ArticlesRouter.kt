package com.aglushkov.wordteacher.shared.features.articles.vm

interface ArticlesRouter {
    fun openAddArticle()
    fun openArticle(id: Long)
}