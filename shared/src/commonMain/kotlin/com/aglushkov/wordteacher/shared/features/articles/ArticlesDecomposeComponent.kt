package com.aglushkov.wordteacher.shared.features.articles

import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext

class ArticlesDecomposeComponent (
    componentContext: ComponentContext,
    articlesRepository: ArticlesRepository,
    timeSource: TimeSource
) : ArticlesVMImpl(
    articlesRepository,
    timeSource
), ComponentContext by componentContext {
}
