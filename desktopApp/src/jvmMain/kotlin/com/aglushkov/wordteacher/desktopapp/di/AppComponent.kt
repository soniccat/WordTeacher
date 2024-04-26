package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.TabComposeComponentDependencies
import com.aglushkov.wordteacher.desktopapp.general.RouterResolver
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.add_article.di.AddArticleDependencies
import com.aglushkov.wordteacher.shared.features.article.di.ArticleDependencies
import com.aglushkov.wordteacher.shared.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.shared.features.cardset.di.CardSetDependencies
import com.aglushkov.wordteacher.shared.features.cardset_info.di.CardSetInfoDependencies
import com.aglushkov.wordteacher.shared.features.cardset_json_import.di.CardSetJsonImportDependencies
import com.aglushkov.wordteacher.shared.features.cardsets.di.CardSetsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.dict_configs.di.DictConfigsDependencies
import com.aglushkov.wordteacher.shared.features.settings.di.SettingsDependencies
import com.aglushkov.wordteacher.shared.features.webauth.di.WebAuthDependencies
import com.aglushkov.wordteacher.shared.general.FileLogger
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import dagger.Component
import io.ktor.client.plugins.cookies.CookiesStorage


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
interface AppComponent:
    DefinitionsDependencies,
    CardSetsDependencies,
    CardSetDependencies,
    CardSetInfoDependencies,
    ArticlesDependencies,
    AddArticleDependencies,
    ArticleDependencies,
    SettingsDependencies,
    DictConfigsDependencies,
    TabComposeComponentDependencies,
    WebAuthDependencies,
    CardSetJsonImportDependencies
{
    fun cookieStorage(): CookiesStorage
    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    fun routerResolver(): RouterResolver
    fun googleAuthController(): GoogleAuthController
    fun fileLogger(): FileLogger

    @IsDebug
    override fun isDebug(): Boolean

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
