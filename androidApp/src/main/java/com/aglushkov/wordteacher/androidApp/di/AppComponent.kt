package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.GApp
import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.androidApp.features.add_article.di.AddArticleDependencies
import com.aglushkov.wordteacher.androidApp.features.article.di.ArticleDependencies
import com.aglushkov.wordteacher.androidApp.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.androidApp.features.cardsets.di.CardSetsDependencies
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Component


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
public interface AppComponent:
    DefinitionsDependencies,
    ArticlesDependencies,
    AddArticleDependencies,
    ArticleDependencies,
    CardSetsDependencies {

    fun configService(): ConfigService
    fun configRepository(): ConfigRepository
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    override fun database(): AppDatabase
    override fun nlpCore(): NLPCore

    override fun routerResolver(): RouterResolver
    override fun articlesRepository(): ArticlesRepository
    override fun cardSetsRepository(): CardSetsRepository
    override fun wordRepository(): WordDefinitionRepository
    override fun idGenerator(): IdGenerator
    override fun timeSource(): TimeSource
    override fun connectivityManager(): ConnectivityManager

    fun injectApplication(app: GApp)

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
