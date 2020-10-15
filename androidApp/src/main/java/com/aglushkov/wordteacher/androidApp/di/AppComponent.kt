package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.androidApp.general.ItemViewBinder
import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Component


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
public interface AppComponent: DefinitionsDependencies {
    override fun getConfigService(): ConfigService
    override fun getConfigRepository(): ConfigRepository
    override fun getConfigConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    override fun getServiceRepository(): ServiceRepository
    override fun getWordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    override fun getWordRepository(): WordRepository

    override fun getConnectivityManager(): ConnectivityManager
}
