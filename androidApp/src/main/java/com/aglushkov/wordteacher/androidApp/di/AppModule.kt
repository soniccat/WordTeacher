package com.aglushkov.wordteacher.di

import android.content.Context
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
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
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @AppComp
    @Provides
    fun createConfigService(context: Context): ConfigService {
        val baseUrl = context.getString(R.string.config_base_url)
        return ConfigService(baseUrl)
    }

    @AppComp
    @Provides
    fun createConfigRepository(configService: ConfigService, connectivityManager: ConnectivityManager): ConfigRepository {
        return ConfigRepository(configService, connectivityManager)
    }

    @AppComp
    @Provides
    fun createConfigConnectParamsStatRepository(context: Context): ConfigConnectParamsStatRepository {
        return ConfigConnectParamsStatRepository(ConfigConnectParamsStatFile(context))
    }

    @AppComp
    @Provides
    fun createServiceRepository(
        configRepository: ConfigRepository,
        configConnectParamsStatRepository: ConfigConnectParamsStatRepository,
        factory: WordTeacherWordServiceFactory
    ): ServiceRepository {
        return ServiceRepository(configRepository, configConnectParamsStatRepository, factory)
    }

    @AppComp
    @Provides
    fun createWordRepository(serviceRepository: ServiceRepository): WordRepository {
        return WordRepository(serviceRepository)
    }

    @AppComp
    @Provides
    fun getWordTeacherWordServiceFactory(): WordTeacherWordServiceFactory {
        return WordTeacherWordServiceFactory()
    }
}