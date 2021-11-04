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
import com.aglushkov.wordteacher.shared.repository.note.NotesRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Module
import dagger.Provides

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
    fun wordRepository(serviceRepository: ServiceRepository): WordDefinitionRepository {
        return WordDefinitionRepository(serviceRepository)
    }

    @AppComp
    @Provides
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory()
    }

    @AppComp
    @Provides
    fun articleRepository(
        database: AppDatabase,
        nlpCore: NLPCore,
        processor: NLPSentenceProcessor
    ): ArticlesRepository {
        return ArticlesRepository(database, nlpCore, processor)
    }

    @AppComp
    @Provides
    fun cardSetsRepository(
        database: AppDatabase
    ): CardSetsRepository {
        return CardSetsRepository(database)
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
    fun nlpSentenceProcessor(nlpCore: NLPCore) = NLPSentenceProcessor(nlpCore)

    @AppComp
    @Provides
    fun database(driver: DatabaseDriverFactory): AppDatabase {
        return AppDatabase(driver)
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
    fun nlpCore(context: Context): NLPCore {
        return NLPCore(
            context.resources,
            R.raw.en_sent,
            R.raw.en_token,
            R.raw.en_pos_maxent,
            R.raw.en_lemmatizer_dict,
            R.raw.en_chunker
        )
    }
}