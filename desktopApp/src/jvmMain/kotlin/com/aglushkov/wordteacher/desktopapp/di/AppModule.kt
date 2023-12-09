//@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.configs.GoogleConfig
import com.aglushkov.wordteacher.desktopapp.configs.KeyStoreConfig
import com.aglushkov.wordteacher.desktopapp.general.crypto.CertCreator
import com.aglushkov.wordteacher.desktopapp.general.crypto.SecureCodecBuilder
import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.ApiBaseUrl
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.BasePath
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.Platform
import com.aglushkov.wordteacher.shared.di.SharedAppModule
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.add_article.vm.toArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.res.MR
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import dagger.Module
import dagger.Provides
import io.ktor.http.Url
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date


@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = true // TODO: figure out how to get isDebug state

    @Platform
    @AppComp
    @Provides
    fun platform(): String = "desktop"

    @BasePath
    @AppComp
    @Provides
    fun basePath(): Path = "../wordTeacherDesktop".toPath()

    @ApiBaseUrl
    @AppComp
    @Provides
    fun apiBaseUrl(): String = MR.strings.api_base_url.localized()

    @AppComp
    @Provides
    fun settings(): FlowSettings {
        val s = PreferencesSettings.Factory().create("settings.preferences_pb")
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
            nlpPath,
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

    @AppComp
    @Provides
    fun cardSetsFeatures(): CardSetsVM.Features {
        return CardSetsVM.Features(
            canImportCardSetFromJson = true,
        )
    }

    @AppComp
    @Provides
    fun secureCodec(
        @BasePath basePath: Path,
        fileSystem: FileSystem,
    ): SecureCodec {
        return SecureCodecBuilder(basePath, fileSystem).build()
    }

    @AppComp
    @Provides
    fun contentExtractors(): Array<ArticleContentExtractor> {
        return arrayOf(
            ArticleParserRepository().toArticleContentExtractor(),
        )
    }

    @AppComp
    @Provides
    fun fileSharer(): FileSharer? {
        return null
    }
}
