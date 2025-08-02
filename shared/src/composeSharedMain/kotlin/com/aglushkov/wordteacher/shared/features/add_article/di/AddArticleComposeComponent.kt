package com.aglushkov.wordteacher.shared.features.add_article.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.add_article.AddArticleDecomposeComponent
import com.aglushkov.wordteacher.shared.features.add_article.vm.AddArticleVM
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [AddArticleDependencies::class], modules = [AddArticleComposeModule::class])
interface AddArticleComposeComponent {
    fun addArticleDecomposeComponent(): AddArticleDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: AddArticleVM.State): Builder

        fun setDeps(deps: AddArticleDependencies): Builder
        fun build(): AddArticleComposeComponent
    }
}

interface AddArticleDependencies {
    fun articlesRepository(): ArticlesRepository
    fun cardSetsRepository(): CardSetsRepository
    fun timeSource(): TimeSource
    fun contentExtractors(): Array<ArticleContentExtractor>
    fun analytics(): Analytics
    fun settingStore(): SettingStore
}
