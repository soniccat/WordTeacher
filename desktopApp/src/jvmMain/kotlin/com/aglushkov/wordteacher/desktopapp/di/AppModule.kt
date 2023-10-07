//@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.aglushkov.wordteacher.desktopapp.di

import com.aglushkov.wordteacher.desktopapp.configs.GoogleConfig
import com.aglushkov.wordteacher.desktopapp.general.crypto.CertAndKeyGen
import com.aglushkov.wordteacher.desktopapp.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.ApiBaseUrl
import com.aglushkov.wordteacher.shared.di.AppComp
import com.aglushkov.wordteacher.shared.di.BasePath
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.di.Platform
import com.aglushkov.wordteacher.shared.di.SharedAppModule
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.AppInfo
import com.aglushkov.wordteacher.shared.general.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
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
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.Security
import java.security.cert.X509Certificate


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
        val keystoreType = "PKCS12"
        val keystoreFilename = "sender_keystore.p12"
        val keystorePassword = "abc"
        val alias = "senderKeyPair"
        val cnString = "CN=Baeldung"
        val rsaKeylength = 2048
        val certificateSignatureAlgorithm = "SHA1WithRSA"
        val certificateValidityDays = 365

        val keyStorePath = basePath.div(keystoreFilename)
        val needInit = !fileSystem.exists(keyStorePath)
        if (needInit) {
            try {
                val keyStore = KeyStore.getInstance(keystoreType)
                keyStore.load(null, null)
                keyStore.store(
                    FileOutputStream(keyStorePath.toString()),
                    keystorePassword.toCharArray()
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        Security.addProvider(BouncyCastleProvider())

        val keyStore = KeyStore.getInstance(keystoreType)
        keyStore.load(FileInputStream(keyStorePath.toString()), keystorePassword.toCharArray())
        if (needInit || keyStore.aliases().toList().isEmpty()) {
            try {
                val gen = CertAndKeyGen("RSA", certificateSignatureAlgorithm)
                gen.generate(rsaKeylength)
                val key = gen.privateKey
                val cert: X509Certificate = gen.getSelfCertificate(
                    X500Name(cnString),
                    certificateValidityDays.toLong() * 1
                )
                val chain: Array<X509Certificate?> = arrayOfNulls<X509Certificate>(1)
                chain[0] = cert
                keyStore.setKeyEntry(alias, key, keystorePassword.toCharArray(), chain)
                keyStore.store(
                    FileOutputStream(keyStorePath.toString()),
                    keystorePassword.toCharArray()
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        } else {
            try {
                val e = keyStore.getEntry(alias, PasswordProtection(keystorePassword.toCharArray()))
                println(e.toString())
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }

        return SecureCodec(keyStore!!, alias, PasswordProtection(keystorePassword.toCharArray()))
    }
}