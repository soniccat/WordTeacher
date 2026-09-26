package com.aglushkov.wordteacher.shared.features.articles.vm

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleVM

interface ArticlesRouter {
    fun openFindArticle()
    fun openArticle(state: ArticleVM.State)
}