package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthRepositoryImpl
import com.aglushkov.wordteacher.shared.di.*
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

@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = true // TODO: figure out how to get isDebug state

    @BasePath
    @AppComp
    @Provides
    fun basePath(): Path = ".".toPath()

    @ConfigBaseUrl
    @AppComp
    @Provides
    fun configBaseUrl(): String = MR.strings.config_base_url.localized()

    @ApiBaseUrl
    @AppComp
    @Provides
    fun apiBaseUrl(): String = MR.strings.api_base_url.localized()

    @AppComp
    @Provides
    fun settings(): FlowSettings {
        val s = JvmPreferencesSettings.Factory().create("settings.preferences_pb") as JvmPreferencesSettings
        return s.toFlowSettings()
    }

    @AppComp
    @Provides
    fun appInfo(): AppInfo = AppInfo("1.0", "Desktop") // TODO: figure out how to get version

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
    ): GoogleAuthRepository = GoogleAuthRepositoryImpl()

    @AppComp
    @Provides
    fun databaseFactory() = DatabaseDriverFactory()

    @AppComp
    @Provides
    fun nlpCore(): NLPCore {
        return NLPCore()
    }

//    @AppComp
//    @Provides
//    fun nlpCore(context: Context, fileSystem: FileSystem): NLPCore {
//        val nlpIndexPath = context.filesDir.absolutePath.toPath().div("nlp")
//        if (!fileSystem.exists(nlpIndexPath)) {
//            fileSystem.createDirectory(nlpIndexPath)
//        }
//        return NLPCore(
//            context.resources,
//            R.raw.en_sent,
//            R.raw.en_token,
//            R.raw.en_pos_maxent,
//            R.raw.en_lemmatizer_dict,
//            R.raw.en_chunker,
//            nlpIndexPath,
//            fileSystem
//        )
//    }
}