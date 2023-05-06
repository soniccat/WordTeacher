package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.di.AppComp
import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthRepositoryImpl
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.deviceid.DeviceIdRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictFactory
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepositoryImpl
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import dagger.Module
import dagger.Provides
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.service.SpaceAuthService
import com.aglushkov.wordteacher.shared.service.SpaceCardSetService
import com.aglushkov.wordteacher.shared.service.SpaceHttpClientBuilder
import com.aglushkov.wordteacher.shared.workers.DatabaseWorker
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.JvmPreferencesSettings
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.russhwolf.settings.coroutines.toSuspendSettings
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Qualifier

@Module
class AppModule {
    @AppComp
    @Provides
    fun configService(): ConfigService {
        return ConfigService(MR.strings.config_base_url.toString())
    }

    @AppComp
    @Provides
    fun configRepository(configService: ConfigService, connectivityManager: ConnectivityManager): ConfigRepository {
        return ConfigRepository(configService, connectivityManager)
    }

    @AppComp
    @Provides
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository {
        return ConfigConnectParamsStatRepository(ConfigConnectParamsStatFile())
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
    fun dictRepository(
        dictFactory: DictFactory,
        fileSystem: FileSystem
    ): DictRepository {
        val dictsPath = ".".toPath().div("dicts")
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
    fun nlpSentenceProcessor(nlpCore: NLPCore) = NLPSentenceProcessor()

    @AppComp
    @Provides
    fun settings(): FlowSettings {
        val s = JvmPreferencesSettings.Factory().create("settings.preferences_pb") as JvmPreferencesSettings
        return s.toFlowSettings()
    }

    @AppComp
    @Provides
    fun deviceIdRepository(
        settings: FlowSettings
    ): DeviceIdRepository =
        DeviceIdRepository(settings)

    @AppComp
    @Provides
    fun cookieStorage(
        fileSystem: FileSystem
    ): CookiesStorage {
        val cookieFilePath = ".".toPath().div("cookies")
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
        true, // TODO: figure out how to get isDebug
    ).build()

    @AppComp
    @Provides
    fun spaceAuthService(
        @SpaceHttpClient httpClient: HttpClient,
    ): SpaceAuthService =
        SpaceAuthService(MR.strings.api_base_url.toString(), httpClient)

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
    ): GoogleAuthRepository = GoogleAuthRepositoryImpl()


    @AppComp
    @Provides
    fun spaceAuthRepository(
        service: SpaceAuthService,
        googleAuthRepository: GoogleAuthRepository,
        fileSystem: FileSystem,
    ): SpaceAuthRepository {
        val path = ".".toPath().div("authData")
        return SpaceAuthRepository(service, googleAuthRepository, path, fileSystem)
    }

    private fun obtainSpaceDirPath(fileSystem: FileSystem): Path {
        val spaceDirPath = ".".toPath().div("space")
        if (!fileSystem.exists(spaceDirPath)) {
            fileSystem.createDirectory(spaceDirPath)
        }

        return spaceDirPath
    }

    @AppComp
    @Provides
    fun appInfo(): AppInfo = AppInfo("1.0", "Desktop") // TODO: figure out how to get version

    @AppComp
    @Provides
    fun spaceCardSetService(
        @SpaceHttpClient httpClient: HttpClient,
    ): SpaceCardSetService =
        SpaceCardSetService(MR.strings.api_base_url.toString(), httpClient)

    @AppComp
    @Provides
    fun database(driver: DatabaseDriverFactory, timeSource: TimeSource): AppDatabase {
        return AppDatabase(driver, timeSource)
    }

    @AppComp
    @Provides
    fun databaseFactory() = DatabaseDriverFactory()

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
    fun nlpCore(): NLPCore {
        return NLPCore()
    }
}

@Qualifier
annotation class SpaceHttpClient