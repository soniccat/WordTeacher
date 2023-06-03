package com.aglushkov.wordteacher.android_app.di

import com.aglushkov.wordteacher.android_app.GApp
import com.aglushkov.wordteacher.android_app.features.learning.di.LearningDependencies
import com.aglushkov.wordteacher.android_app.features.learning_session_result.di.LearningSessionResultDependencies
import com.aglushkov.wordteacher.android_app.features.notes.di.NotesDependencies
import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.SpaceHttpClient
import com.aglushkov.wordteacher.shared.features.add_article.di.AddArticleDependencies
import com.aglushkov.wordteacher.shared.features.article.di.ArticleDependencies
import com.aglushkov.wordteacher.shared.features.articles.di.ArticlesDependencies
import com.aglushkov.wordteacher.shared.features.cardset.di.CardSetDependencies
import com.aglushkov.wordteacher.shared.features.cardsets.di.CardSetsDependencies
import com.aglushkov.wordteacher.shared.features.definitions.di.DefinitionsDependencies
import com.aglushkov.wordteacher.shared.features.settings.di.SettingsDependencies
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
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.Component
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
interface AppComponent:
    DefinitionsDependencies,
    ArticlesDependencies,
    AddArticleDependencies,
    ArticleDependencies,
    CardSetsDependencies,
    CardSetDependencies,
    NotesDependencies,
    LearningDependencies,
    LearningSessionResultDependencies,
    SettingsDependencies {

    override fun settings(): FlowSettings
    fun configService(): ConfigService
    fun configRepository(): ConfigRepository
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    fun serviceRepository(): ServiceRepository
    override fun dictRepository(): DictRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    override fun database(): AppDatabase
    override fun databaseWorker(): DatabaseWorker
    override fun databaseCardSetWorker(): DatabaseCardWorker

    override fun nlpCore(): NLPCore

    override fun routerResolver(): RouterResolver
    override fun articlesRepository(): ArticlesRepository
    override fun cardSetsRepository(): CardSetsRepository
    override fun cardSetSearchRepository(): CardSetSearchRepository
    override fun wordRepository(): WordDefinitionRepository
    override fun notesRepository(): NotesRepository

    fun googleAuthRepository(): GoogleAuthControllerImpl
    fun cookieStorage(): CookiesStorage
    @SpaceHttpClient
    fun spaceHttpClient(): HttpClient
    fun appInfo(): AppInfo
    fun deviceIdRepository(): DeviceIdRepository
    fun spaceAuthService(): SpaceAuthService
    fun spaceCardSetService(): SpaceCardSetService
    override fun spaceAuthRepository(): SpaceAuthRepository

    override fun idGenerator(): IdGenerator
    override fun timeSource(): TimeSource
    override fun connectivityManager(): ConnectivityManager
    fun injectApplication(app: GApp)

    @IsDebug fun isDebug(): Boolean

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
