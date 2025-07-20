package com.aglushkov.wordteacher.shared.di

import com.aglushkov.wordteacher.shared.analytics.AnalyticEngine
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.apiproviders.wordteacher.WordTeacherDictService
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetVM
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.VKAuthController
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthController
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.SpaceHttpClientBuilder
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardEnricher
import com.aglushkov.wordteacher.shared.repository.cardset.CardEnricherImpl
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.clipboard.ClipboardRepository
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigConnectParams
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadCardSetRepository
import com.aglushkov.wordteacher.shared.repository.dashboard.ReadHeadlineRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyGradationProvider
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
import com.aglushkov.wordteacher.shared.repository.toggles.ToggleRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionHistoryRepository
import com.aglushkov.wordteacher.shared.service.*
import com.aglushkov.wordteacher.shared.workers.CardFrequencyUpdateWorker
import com.aglushkov.wordteacher.shared.workers.CardSetSyncWorker
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import com.aglushkov.wordteacher.shared.workers.SpanUpdateWorker
import com.russhwolf.settings.coroutines.FlowSettings
import dagger.Lazy
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
        fileSystem: FileSystem,
        secureCodec: SecureCodec,
    ): ConfigRepository {
        val configPath = basePath.div("services")
        val wordTeacherDictServiceConfig = Config(0, Config.Type.WordTeacher, ConfigConnectParams("", "", ""), emptyMap())
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
    @DictPath
    fun dictPath(
        @BasePath basePath: Path,
    ): Path {
        return basePath.div("dicts")
    }

    @AppComp
    @Provides
    fun dictRepository(
        @DictPath dictPath: Path,
        dictFactory: DictFactory,
        fileSystem: FileSystem
    ): DictRepository {
        if (!fileSystem.exists(dictPath)) {
            fileSystem.createDirectory(dictPath)
        }
        return DictRepositoryImpl(dictPath, dictFactory, fileSystem)
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
        wordTeacherDictService: WordTeacherDictService,
        secureCodec: SecureCodec,
    ): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory(wordTeacherDictService, secureCodec)
    }

    @AppComp
    @Provides
    fun articlesRepository(
        database: AppDatabase,
        nlpCore: NLPCore,
        processor: NLPSentenceProcessor,
        settings: SettingStore,
        timeSource: TimeSource,
        @IsDebug isDebug: Boolean,
    ): ArticlesRepository {
        return ArticlesRepository(database, nlpCore, processor, settings, timeSource, isDebug)
    }

    @AppComp
    @Provides
    fun cardSetsRepository(
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        nlpCore: NLPCore,
        nlpSentenceProcessor: NLPSentenceProcessor,
    ): CardSetsRepository {
        return CardSetsRepository(databaseWorker, timeSource, nlpCore, nlpSentenceProcessor)
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
    fun wordFrequencyDatabase(
        driver: DatabaseDriverFactory,
        @WordFrequencyPreparer dbPreparer: () -> Path,
        settings: SettingStore,
    ): WordFrequencyDatabase {
        return WordFrequencyDatabase(driver, dbPreparer, settings)
    }

    @AppComp
    @Provides
    fun wordFrequencyGradationProvider(
        db: WordFrequencyDatabase,
    ): WordFrequencyGradationProvider {
        return db // TODO: user dagger bind instead
    }

    @AppComp
    @Provides
    fun databaseCardWorker(
        databaseWorker: DatabaseWorker,
        spanUpdateWorker: SpanUpdateWorker,
        cardSetSyncWorker: CardSetSyncWorker,
        cardFrequencyUpdateWorker: CardFrequencyUpdateWorker,
    ): DatabaseCardWorker {
        return DatabaseCardWorker(databaseWorker, spanUpdateWorker, cardSetSyncWorker, cardFrequencyUpdateWorker)
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
    fun cardFrequencyUpdateWorker(
        databaseWorker: DatabaseWorker,
        frequencyDatabase: WordFrequencyDatabase,
    ): CardFrequencyUpdateWorker {
        return CardFrequencyUpdateWorker(databaseWorker, frequencyDatabase)
    }

    @AppComp
    @Provides
    fun cardSetSyncWorker(
        spaceAuthRepository: SpaceAuthRepository,
        spaceCardSetService: SpaceCardSetService,
        database: AppDatabase,
        databaseWorker: DatabaseWorker,
        timeSource: TimeSource,
        settings: SettingStore,
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
        spaceAuthRepository: Lazy<SpaceAuthRepository>,
        secureCodec: SecureCodec,
        @IsDebug isDebug: Boolean,
        analytics: Lazy<Analytics>,
    ): HttpClient = SpaceHttpClientBuilder(
        deviceIdRepository,
        appInfo,
        cookieStorage,
        { spaceAuthRepository.get() },
        secureCodec,
        isDebug,
        { analytics.get() },
    ).build()

    @AppComp
    @Provides
    fun deviceIdRepository(
        settings: SettingStore
    ): DeviceIdRepository =
        DeviceIdRepository(settings)

    @AppComp
    @Provides
    fun spaceDashboardService(
        @SpaceHttpClient httpClient: HttpClient,
        @ApiBaseUrl apiBaseUrl: String,
    ): SpaceDashboardService =
        SpaceDashboardService(apiBaseUrl, httpClient)

    @AppComp
    @Provides
    fun spaceAuthService(
        @SpaceHttpClient httpClient: HttpClient,
        @ApiBaseUrl apiBaseUrl: String,
        secureCodec: SecureCodec,
    ): SpaceAuthService =
        SpaceAuthService(apiBaseUrl, httpClient, secureCodec)

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
        vkAuthController: VKAuthController,
        yandexAuthController: YandexAuthController,
        fileSystem: FileSystem,
    ): SpaceAuthRepository {
        val path = obtainSpaceDirPath(basePath,fileSystem).div("authData")
        return SpaceAuthRepository(
            service,
            googleAuthController,
            vkAuthController,
            yandexAuthController,
            path,
            fileSystem,
        )
    }

    @AppComp
    @Provides
    fun wordTeacherDictService(
        @ApiBaseUrl apiBaseUrl: String,
        deviceIdRepository: DeviceIdRepository,
        appInfo: AppInfo,
        @IsDebug isDebug: Boolean,
        analytics: Lazy<Analytics>,
    ): WordTeacherDictService {
        return WordTeacherDictService(
            baseUrl = apiBaseUrl,
            deviceIdRepository = deviceIdRepository,
            appInfo = appInfo,
            isDebug = isDebug,
            analyticsProvider = { analytics.get() }
        )
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
        logsRepository: LogsRepository,
    ): FileLogger {
        val dirPath = basePath.div("logs")
        if (!fileSystem.exists(dirPath)) {
            fileSystem.createDirectory(dirPath)
        }

        return FileLogger(
            dirPath,
            fileSystem,
            timeSource,
            isEnabledProvider = { logsRepository.isLoggingEnabledState.value }
        )
    }

    @AppComp
    @Provides
    fun logsRepository(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
        settings: SettingStore
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

    @AppComp
    @Provides
    fun analytics(
        analyticEngines: Array<AnalyticEngine>
    ): Analytics {
        return Analytics(engines = analyticEngines.asList())
    }

    @AppComp
    @Provides
    fun clipboardRepository(): ClipboardRepository {
        return ClipboardRepository()
    }

    @AppComp
    @Provides
    fun wordDefinitionHistoryRepository(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
    ): WordDefinitionHistoryRepository {
        val filePath = basePath.div("wordhistory")
        return WordDefinitionHistoryRepository(filePath, fileSystem)
    }

    @AppComp
    @Provides
    fun readHeadlineHistoryRepository(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
    ): ReadHeadlineRepository {
        val filePath = basePath.div("readHeadlineHistory")
        return ReadHeadlineRepository(filePath, fileSystem)
    }

    @AppComp
    @Provides
    fun readCardSetHistoryRepository(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
    ): ReadCardSetRepository {
        val filePath = basePath.div("readCardSetHistory")
        return ReadCardSetRepository(filePath, fileSystem)
    }

    @AppComp
    @Provides
    fun cardEnricher(
        wordTeacherDictService: WordTeacherDictService,
        nlpCore: NLPCore
    ): CardEnricher {
        return CardEnricherImpl(wordTeacherDictService, nlpCore)
    }

    @AppComp
    @Provides
    fun cardSetFeatures(
        @IsDebug isDebug: Boolean,
    ): CardSetVM.Features {
        return CardSetVM.Features(
            canEnrich = isDebug,
        )
    }

    @AppComp
    @Provides
    fun toggleRepository(
        @ToggleUrl toggleUrl: String,
        @ToggleUrl2 toggleUrl2: String,
        @SpaceHttpClient httpClient: HttpClient,
        settings: SettingStore
    ): ToggleRepository {
        return ToggleRepository(
            toggleUrl,
            toggleUrl2,
            httpClient,
            settings
        )
    }
}
