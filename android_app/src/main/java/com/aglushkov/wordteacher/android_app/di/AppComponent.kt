package com.aglushkov.wordteacher.android_app.di

import com.aglushkov.wordteacher.android_app.GApp
import com.aglushkov.wordteacher.android_app.GAppNonMainProccess
import com.aglushkov.wordteacher.android_app.features.learning.di.LearningDependencies
import com.aglushkov.wordteacher.android_app.features.learning_session_result.di.LearningSessionResultDependencies
import com.aglushkov.wordteacher.android_app.features.notes.di.NotesDependencies
import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.android_app.helper.VKAuthControllerImpl
import com.aglushkov.wordteacher.android_app.helper.WebLinkOpenerImpl
import com.aglushkov.wordteacher.android_app.helper.YandexAuthControllerImpl
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.Platform
import com.aglushkov.wordteacher.shared.di.SpaceHttpClient
import com.aglushkov.wordteacher.shared.features.add_article.di.AddArticleDependencies
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.article.di.ArticleDependencies
import com.aglushkov.wordteacher.shared.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.shared.features.cardset.di.CardSetDependencies
import com.aglushkov.wordteacher.shared.features.cardset_info.di.CardSetInfoDependencies
import com.aglushkov.wordteacher.shared.features.cardset_json_import.di.CardSetJsonImportDependencies
import com.aglushkov.wordteacher.shared.features.cardsets.di.CardSetsDependencies
import com.aglushkov.wordteacher.shared.features.dashboard.di.DashboardDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.dict_configs.di.DictConfigsDependencies
import com.aglushkov.wordteacher.shared.features.settings.di.SettingsDependencies
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import dagger.Component
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
interface AppComponent:
    DashboardDependencies,
    DefinitionsDependencies,
    ArticlesDependencies,
    AddArticleDependencies,
    ArticleDependencies,
    CardSetsDependencies,
    CardSetDependencies,
    CardSetInfoDependencies,
    DictConfigsDependencies,
    NotesDependencies,
    LearningDependencies,
    LearningSessionResultDependencies,
    CardSetJsonImportDependencies,
    SettingsDependencies {

    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory

    fun googleAuthRepository(): GoogleAuthControllerImpl
    fun vkAuthController(): VKAuthControllerImpl
    fun yandexAuthController(): YandexAuthControllerImpl
    fun cookieStorage(): CookiesStorage
    @SpaceHttpClient
    fun spaceHttpClient(): HttpClient
    fun deviceIdRepository(): DeviceIdRepository
    fun spaceAuthService(): SpaceAuthService
    override fun analytics(): Analytics
    fun webLinkOpenerImpl(): WebLinkOpenerImpl

    fun injectApplication(app: GApp)
    fun injectAppNonMainProccess(app: GAppNonMainProccess)

    @IsDebug fun isDebug(): Boolean
    @Platform fun platform(): String

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
