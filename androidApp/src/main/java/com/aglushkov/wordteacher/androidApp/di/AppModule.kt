package com.aglushkov.wordteacher.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.TimeSourceImpl
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.model.nlp.NLPSentenceProcessor
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.db.DatabaseWorker
import com.aglushkov.wordteacher.shared.repository.dict.DictFactory
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepositoryImpl
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import okio.FileSystem
import okio.Path
import dagger.Module
import dagger.Provides
import kotlin.io.path.div
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