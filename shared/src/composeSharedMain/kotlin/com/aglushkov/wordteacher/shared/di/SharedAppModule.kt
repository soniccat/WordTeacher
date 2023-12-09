package com.aglushkov.wordteacher.shared.di

import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.SpaceHttpClientBuilder
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.cardsetsearch.CardSetSearchRepository
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.aglushkov.wordteacher.shared.repository.dict.DictFactory
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepositoryImpl
import com.aglushkov.wordteacher.shared.repository.logs.LogsRepository
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.service.*
import com.aglushkov.wordteacher.shared.workers.CardSetSyncWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.SpanUpdateWorker
import com.russhwolf.settings.coroutines.FlowSettings
import okio.FileSystem
import dagger.Module
import dagger.Provides
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import okio.Path

@Module
class SharedAppModule {
    @AppComp
    @Provides
    fun configRepository(
        @BasePath basePath: Path,
        @ApiBaseUrl apiBaseUrl: String,
        fileSystem: FileSystem,
        secureCodec: SecureCodec,
    ): ConfigRepository {
        val configPath = basePath.div("services")
        val wordTeacherDictServiceConfig = Config(0, Config.Type.WordTeacher, ConfigConnectParams(apiBaseUrl, "", ""), emptyMap())
        return ConfigRepository(
            configPath,
            fileSystem,
            wordTeacherDictServiceConfig,
            secureCodec,
        )
    }

    @AppComp
    @Provides
    fun serviceRepository(
        configRepository: ConfigRepository,
        factory: WordTeacherWordServiceFactory
    ): ServiceRepository {
        return ServiceRepository(configRepository, factory)
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
    fun dictRepository(
        @BasePath basePath: Path,
        dictFactory: DictFactory,
        fileSystem: FileSystem
    ): DictRepository {
        val dictsPath = basePath.div("dicts")
        if (!fileSystem.exists(dictsPath)) {
            fileSystem.createDirectory(dictsPath)
        }
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
    fun wordTeacherWordServiceFactory(
        secureCodec: SecureCodec,
    ): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory(secureCodec)
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
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        nlpCore: NLPCore,
        nlpSentenceProcessor: NLPSentenceProcessor,
        cardSetService: SpaceCardSetService,
    ): CardSetsRepository {
        return CardSetsRepository(databaseWorker, timeSource, nlpCore, nlpSentenceProcessor, cardSetService)
    }

    @AppComp
    @Provides
    fun cardSetSearchRepository(
        service: SpaceCardSetSearchService,
    ): CardSetSearchRepository {
        return CardSetSearchRepository(service)
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
        databaseWorker: DatabaseWorker,
        spanUpdateWorker: SpanUpdateWorker,
        cardSetSyncWorker: CardSetSyncWorker
    ): DatabaseCardWorker {
        return DatabaseCardWorker(databaseWorker, spanUpdateWorker, cardSetSyncWorker)
    }

    @AppComp
    @Provides
    fun spanUpdateWorker(
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        nlpCore: NLPCore,
        nlpSentenceProcessor: NLPSentenceProcessor,
        timeSource: TimeSource
    ): SpanUpdateWorker {
        return SpanUpdateWorker(database, databaseWorker, nlpCore, nlpSentenceProcessor, timeSource)
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
        @BasePath basePath: Path,
        fileSystem: FileSystem
    ): CookiesStorage {
        val cookieFilePath = basePath.div("cookies")
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
        @Platform platform: String,
        @IsDebug isDebug: Boolean,
    ): HttpClient = SpaceHttpClientBuilder(
        deviceIdRepository,
        appInfo,
        cookieStorage,
        { spaceAuthRepository.get() },
        platform,
        isDebug,
    ).build()

    @AppComp
    @Provides
    fun deviceIdRepository(
        settings: FlowSettings
    ): DeviceIdRepository =
        DeviceIdRepository(settings)

    @AppComp
    @Provides
    fun spaceAuthService(
        @SpaceHttpClient httpClient: HttpClient,
        @ApiBaseUrl apiBaseUrl: String,
    ): SpaceAuthService =
        SpaceAuthService(apiBaseUrl, httpClient)

    @AppComp
    @Provides
    fun spaceCardSetService(
        @SpaceHttpClient httpClient: HttpClient,
        @ApiBaseUrl apiBaseUrl: String,
    ): SpaceCardSetService =
        SpaceCardSetService(apiBaseUrl, httpClient)

    @AppComp
    @Provides
    fun spaceCardSetSearchService(
        @SpaceHttpClient httpClient: HttpClient,
        @ApiBaseUrl apiBaseUrl: String,
    ): SpaceCardSetSearchService =
        SpaceCardSetSearchService(apiBaseUrl, httpClient)

    @AppComp
    @Provides
    fun spaceAuthRepository(
        @BasePath basePath: Path,
        service: SpaceAuthService,
        googleAuthController: GoogleAuthController,
        fileSystem: FileSystem,
    ): SpaceAuthRepository {
        val path = obtainSpaceDirPath(basePath,fileSystem).div("authData")
        return SpaceAuthRepository(service, googleAuthController, path, fileSystem)
    }

    private fun obtainSpaceDirPath(basePath: Path, fileSystem: FileSystem): Path {
        val spaceDirPath = basePath.div("space")
        if (!fileSystem.exists(spaceDirPath)) {
            fileSystem.createDirectory(spaceDirPath)
        }

        return spaceDirPath
    }

    @AppComp
    @Provides
    fun databaseWorker(
        database: AppDatabase
    ): DatabaseWorker {
        return DatabaseWorker(database)
    }

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
    fun fileLogger(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
        timeSource: TimeSource,
    ): FileLogger {
        val dirPath = basePath.div("logs")
        if (!fileSystem.exists(dirPath)) {
            fileSystem.createDirectory(dirPath)
        }

        return FileLogger(
            dirPath,
            fileSystem,
            timeSource,
        )
    }

    @AppComp
    @Provides
    fun logsRepository(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
        settings: FlowSettings
    ): LogsRepository {
        val dirPath = basePath.div("logs")
        if (!fileSystem.exists(dirPath)) {
            fileSystem.createDirectory(dirPath)
        }

        return LogsRepository(
            settings,
            dirPath,
            fileSystem,
        )
    }
}
