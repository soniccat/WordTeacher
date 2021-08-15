package com.aglushkov.wordteacher.shared.features.articles

import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVM
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesVMImpl
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.statekeeper.consume

class ArticlesDecomposeComponent (
    componentContext: ComponentContext,
    articlesRepository: ArticlesRepository,
    timeSource: TimeSource,
    router: ArticlesRouter
) : ArticlesVMImpl(
    articlesRepository,
    timeSource,
    router
), ComponentContext by componentContext {
}