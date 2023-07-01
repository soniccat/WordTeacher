package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.configs.GoogleConfig
import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.*
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.crypto.PkceGenerator
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import dagger.Module
import dagger.Provides
import com.aglushkov.wordteacher.shared.res.MR
import com.russhwolf.settings.JvmPreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import io.ktor.http.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = true // TODO: figure out how to get isDebug state

    @BasePath
    @AppComp
    @Provides
    fun basePath(): Path = "./wordTeacherDesktop".toPath()

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
    ): GoogleAuthController = GoogleAuthControllerImpl()

    @AppComp
    @Provides
    fun databaseFactory(
        @BasePath basePath: Path,
    ) = DatabaseDriverFactory(basePath.toString())

    @AppComp
    @Provides
    fun nlpCore(
        @BasePath basePath: Path,
        fileSystem: FileSystem
    ): NLPCore {
        val nlpPath = basePath.div("nlp")
        if (!fileSystem.exists(nlpPath)) {
            fileSystem.createDirectory(nlpPath)
        }
        return NLPCore(
            nlpPath.div("en_sent.bin"),
            nlpPath.div("en_token.bin"),
            nlpPath.div("en_pos_maxent.bin"),
            nlpPath.div("en_lemmatizer_dict.bin"),
            nlpPath.div("en_chunker.bin"),
            fileSystem
        )
    }

    @AppComp
    @Provides
    fun googleOAuth2Service(): OAuth2Service {
        return OAuth2Service(
            authUrl = Url("https://accounts.google.com/o/oauth2/v2/auth"),
            tokenUrl = Url("https://oauth2.googleapis.com/token"),
            clientId = GoogleConfig.clientId,
            clientSecret = GoogleConfig.secret,
            redirectUrl = Url(GoogleConfig.redirectUri),
            scope = GoogleConfig.scope,
        )
    }
}