package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Component


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
public interface AppComponent: DefinitionsDependencies {
    fun configService(): ConfigService
    fun configRepository(): ConfigRepository
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    fun nlpCore(): NLPCore
    override fun wordRepository(): WordDefinitionRepository
    override fun idGenerator(): IdGenerator
    override fun connectivityManager(): ConnectivityManager
}
