package com.aglushkov.wordteacher.shared.features.articles

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVMImpl
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class ArticlesDecomposeComponent (
    componentContext: ComponentContext,
    articlesRepository: ArticlesRepository,
    timeSource: TimeSource,
    analytics: Analytics
) : ArticlesVMImpl(
    articlesRepository,
    timeSource
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_Articles"

    init {
        baseInit(analytics)
    }
}
