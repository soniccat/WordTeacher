package com.aglushkov.wordteacher.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDefinitionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDividerBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordExampleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordPartOfSpeechBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSubHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSynonymBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTitleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTranscriptionBlueprint
import com.aglushkov.wordteacher.androidApp.general.ItemViewBinder
import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.ConfigConnectParamsStatFile
import com.aglushkov.wordteacher.shared.repository.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    fun wordRepository(serviceRepository: ServiceRepository): WordRepository {
        return WordRepository(serviceRepository)
    }

    @AppComp
    @Provides
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory()
    }

    @AppComp
    @Provides
    fun idGenerator(): IdGenerator {
        return IdGenerator()
    }
}