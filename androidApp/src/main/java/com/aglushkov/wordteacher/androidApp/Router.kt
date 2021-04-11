package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter

interface Router: ArticlesRouter, ArticleRouter, CardSetsRouter