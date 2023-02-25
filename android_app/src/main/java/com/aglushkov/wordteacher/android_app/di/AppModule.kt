package com.aglushkov.wordteacher.android_app.di

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aglushkov.wordteacher.android_app.BuildConfig
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthRepositoryImpl
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.service.SpaceHttpClientBuilder
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.aglushkov.wordteacher.shared.repository.dict.DictFactory
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepositoryImpl
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.*
import com.aglushkov.wordteacher.shared.workers.CardSetSyncWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.SpanUpdateWorker
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.FileSystem
import dagger.Module
import dagger.Provides
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import okio.Path
import okio.Path.Companion.toPath

@Module
class AppModule {
    @AppComp
    @Provides
    fun configService(context: Context): ConfigService {
        val baseUrl = context.getString(R.string.config_base_url)
        return ConfigService(baseUrl)
    }

    @AppComp
    @Provides
    fun configRepository(configService: ConfigService, connectivityManager: ConnectivityManager): ConfigRepository {
        return ConfigRepository(configService, connectivityManager)
    }

    @AppComp
    @Provides
    fun configConnectParamsStatRepository(context: Context): ConfigConnectParamsStatRepository {
        return ConfigConnectParamsStatRepository(ConfigConnectParamsStatFile(context))
    }

    @AppComp
    @Provides
    fun serviceRepository(
        configRepository: ConfigRepository,
        configConnectParamsStatRepository: ConfigConnectParamsStatRepository,
        factory: WordTeacherWordServiceFactory
    ): ServiceRepository {
        return ServiceRepository(configRepository, configConnectParamsStatRepository, factory)
    }

    @AppComp
    @Provides
    fun fileSystem(): FileSystem {
        return FileSystem.SYSTEM
    }

    @AppComp
    @Provides
    fun dictFactory(
        fileSystem: FileSystem
    ): DictFactory {
        return DictFactory(fileSystem)
    }

    @AppComp
    @Provides
    fun settings(
        context: Context
    ): FlowSettings {
        return DataStoreSettings(
            PreferenceDataStoreFactory.create {
                context.dataStoreFile("settings.preferences_pb")
            }
        )
    }

    @AppComp
    @Provides
    fun dictRepository(
        context: Context,
        dictFactory: DictFactory,
        fileSystem: FileSystem
    ): DictRepository {
        val dictsPath = context.filesDir.absolutePath.toPath().div("dicts")
        fileSystem.createDirectory(dictsPath)
        return DictRepositoryImpl(dictsPath, dictFactory, fileSystem)
    }

    @AppComp
    @Provides
    fun wordRepository(
        serviceRepository: ServiceRepository,
        dictRepository: DictRepository,
        nlpCore: NLPCore
    ): WordDefinitionRepository {
        return WordDefinitionRepository(serviceRepository, dictRepository, nlpCore)
    }

    @AppComp
    @Provides
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory()
    }

    @AppComp
    @Provides
    fun articlesRepository(
        database: AppDatabase,
        nlpCore: NLPCore,
        processor: NLPSentenceProcessor,
        timeSource: TimeSource,
    ): ArticlesRepository {
        return ArticlesRepository(database, nlpCore, processor, timeSource)
    }

    @AppComp
    @Provides
    fun cardSetsRepository(
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        nlpCore: NLPCore,
        nlpSentenceProcessor: NLPSentenceProcessor,
    ): CardSetsRepository {
        return CardSetsRepository(database, databaseWorker, timeSource, nlpCore, nlpSentenceProcessor)
    }

    @AppComp
    @Provides
    fun notesRepository(
        database: AppDatabase,
        nlpCore: NLPCore,
    ): NotesRepository {
        return NotesRepository(database, nlpCore)
    }

    @AppComp
    @Provides
    fun nlpSentenceProcessor(nlpCore: NLPCore) = NLPSentenceProcessor()

    @AppComp
    @Provides
    fun database(driver: DatabaseDriverFactory, timeSource: TimeSource): AppDatabase {
        return AppDatabase(driver, timeSource)
    }

    @AppComp
    @Provides
    fun databaseCardWorker(
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        spanUpdateWorker: SpanUpdateWorker,
        cardSetSyncWorker: CardSetSyncWorker
    ): DatabaseCardWorker {
        return DatabaseCardWorker(database, databaseWorker, spanUpdateWorker, cardSetSyncWorker)
    }

    @AppComp
    @Provides
    fun spanUpdateWorker(
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        nlpCore: NLPCore,
        nlpSentenceProcessor: NLPSentenceProcessor
    ): SpanUpdateWorker {
        return SpanUpdateWorker(database, databaseWorker, nlpCore, nlpSentenceProcessor)
    }

    @AppComp
    @Provides
    fun cardSetSyncWorker(
        spaceAuthRepository: SpaceAuthRepository,
        spaceCardSetService: SpaceCardSetService,
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        settings: FlowSettings,
    ): CardSetSyncWorker {
        return CardSetSyncWorker(
            spaceAuthRepository,
            spaceCardSetService,
            database,
            databaseWorker,
            timeSource,
            settings,
        )
    }

    @AppComp
    @Provides
    fun cookieStorage(
        context: Context,
        fileSystem: FileSystem
    ): CookiesStorage {
        val cookieFilePath = context.filesDir.absolutePath.toPath().div("cookies")
        return FileCookieStorage(
            fileSystem,
            cookieFilePath
        )
    }

    @AppComp
    @Provides
    @SpaceHttpClient
    fun spaceHttpClient(
        deviceIdRepository: DeviceIdRepository,
        appInfo: AppInfo,
        cookieStorage: CookiesStorage,
        spaceAuthRepository: dagger.Lazy<SpaceAuthRepository>,
    ): HttpClient = SpaceHttpClientBuilder(
        deviceIdRepository,
        appInfo,
        cookieStorage,
        { spaceAuthRepository.get() },
        BuildConfig.DEBUG,
    ).build()

    @AppComp
    @Provides
    fun appInfo(): AppInfo = AppInfo(BuildConfig.VERSION_NAME, "Android")

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
        context: Context
    ): GoogleAuthRepositoryImpl =
        GoogleAuthRepositoryImpl(context.getString(R.string.default_web_client_id))

    @AppComp
    @Provides
    fun googleAuthRepository(
        impl: GoogleAuthRepositoryImpl
    ): GoogleAuthRepository = impl

    @AppComp
    @Provides
    fun deviceIdRepository(
        settings: FlowSettings
    ): DeviceIdRepository =
        DeviceIdRepository(settings)

    @AppComp
    @Provides
    fun spaceAuthService(
        context: Context,
        @SpaceHttpClient httpClient: HttpClient,
    ): SpaceAuthService =
        SpaceAuthService(context.getString(R.string.api_base_url), httpClient)

    @AppComp
    @Provides
    fun spaceCardSetService(
        context: Context,
        @SpaceHttpClient httpClient: HttpClient,
    ): SpaceCardSetService =
        SpaceCardSetService(context.getString(R.string.api_base_url_cardsets), httpClient)

    @AppComp
    @Provides
    fun spaceAuthRepository(
        context: Context,
        service: SpaceAuthService,
        googleAuthRepository: GoogleAuthRepository,
        fileSystem: FileSystem,
    ): SpaceAuthRepository {
        val path = obtainSpaceDirPath(context, fileSystem).div("authData")
        return SpaceAuthRepository(service, googleAuthRepository, path, fileSystem)
    }

    private fun obtainSpaceDirPath(context: Context, fileSystem: FileSystem): Path {
        val spaceDirPath = context.filesDir.absolutePath.toPath().div("space")
        if (!fileSystem.exists(spaceDirPath)) {
            fileSystem.createDirectory(spaceDirPath)
        }

        return spaceDirPath
    }

    @AppComp
    @Provides
    fun databaseWorker(): DatabaseWorker {
        return DatabaseWorker()
    }

    @AppComp
    @Provides
    fun databaseFactory(context: Context) = DatabaseDriverFactory(context)

    @AppComp
    @Provides
    fun idGenerator(): IdGenerator {
        return IdGenerator()
    }

    @AppComp
    @Provides
    fun timeSource(): TimeSource {
        return TimeSourceImpl()
    }

    @AppComp
    @Provides
    fun nlpCore(context: Context, fileSystem: FileSystem): NLPCore {
        val nlpIndexPath = context.filesDir.absolutePath.toPath().div("nlp")
        if (!fileSystem.exists(nlpIndexPath)) {
            fileSystem.createDirectory(nlpIndexPath)
        }
        return NLPCore(
            context.resources,
            R.raw.en_sent,
            R.raw.en_token,
            R.raw.en_pos_maxent,
            R.raw.en_lemmatizer_dict,
            R.raw.en_chunker,
            nlpIndexPath,
            fileSystem
        )
    }
}