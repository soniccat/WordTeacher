package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.features.definitions.di.DefinitionsComposeDependencies
import com.aglushkov.wordteacher.desktopapp.general.RouterResolver
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.service.ConfigConnectParamsStatRepository
import com.aglushkov.wordteacher.shared.repository.service.ServiceRepository
import com.aglushkov.wordteacher.shared.repository.service.WordTeacherWordServiceFactory
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.service.ConfigService
import dagger.Component


@AppComp
@Component(modules = [AppModule::class, GeneralModule::class] )
public interface AppComponent:
    //DefinitionsDependencies,
    DefinitionsComposeDependencies {

    fun configService(): ConfigService
    fun configRepository(): ConfigRepository
    fun configConnectParamsStatRepository(): ConfigConnectParamsStatRepository
    fun serviceRepository(): ServiceRepository
    fun wordTeacherWordServiceFactory(): WordTeacherWordServiceFactory
    fun database(): AppDatabase
    fun nlpCore(): NLPCore

    override fun dictRepository(): DictRepository
    override fun routerResolver(): RouterResolver
//    fun articlesRepository(): ArticlesRepository
    override fun cardSetsRepository(): CardSetsRepository
    override fun wordRepository(): WordDefinitionRepository
    override fun idGenerator(): IdGenerator
    fun timeSource(): TimeSource
    override fun connectivityManager(): ConnectivityManager

    @Component.Builder
    interface Builder {
        fun generalModule(module: GeneralModule): Builder
        fun appModule(module: AppModule): Builder

        fun build(): AppComponent
    }
}
