package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.TabComposeComponentDependencies
import com.aglushkov.wordteacher.desktopapp.general.RouterResolver
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.add_article.di.AddArticleDependencies
import com.aglushkov.wordteacher.shared.features.article.di.ArticleDependencies
import com.aglushkov.wordteacher.shared.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.shared.features.cardset.di.CardSetDependencies
import com.aglushkov.wordteacher.shared.features.cardset_json_import.di.CardSetJsonImportDependencies
import com.aglushkov.wordteacher.shared.features.cardsets.di.CardSetsDependencies
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.settings.di.SettingsDependencies
import com.aglushkov.wordteacher.shared.features.webauth.di.WebAuthDependencies
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Component
import io.ktor.client.plugins.cookies.CookiesStorage


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
interface AppComponent:
    DefinitionsDependencies,
    CardSetsDependencies,
    CardSetDependencies,
    ArticlesDependencies,
    AddArticleDependencies,
    ArticleDependencies,
    SettingsDependencies,
    TabComposeComponentDependencies,
    WebAuthDependencies,
    CardSetJsonImportDependencies
{
    fun cookieStorage(): CookiesStorage
    fun configService(): ConfigService
    fun configRepository(): ConfigRepository
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    fun routerResolver(): RouterResolver
    fun googleAuthController(): GoogleAuthController

    @IsDebug
    override fun isDebug(): Boolean

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
