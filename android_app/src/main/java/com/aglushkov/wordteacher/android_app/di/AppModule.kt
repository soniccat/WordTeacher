package com.aglushkov.wordteacher.android_app.di

import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aglushkov.wordteacher.android_app.BuildConfig
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.*
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.res.MR
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.FileSystem
import dagger.Module
import dagger.Provides
import okio.Path
import okio.Path.Companion.toPath
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.security.auth.x500.X500Principal

@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = BuildConfig.DEBUG

    @Platform
    @AppComp
    @Provides
    fun platform(): String = "android"

    @BasePath
    @AppComp
    @Provides
    fun basePath(
        context: Context
    ): Path = context.filesDir.absolutePath.toPath()

    @ApiBaseUrl
    @AppComp
    @Provides
    fun apiBaseUrl(
        context: Context
    ): String = context.getString(MR.strings.api_base_url.resourceId)

    @AppComp
    @Provides
    fun settings(
        context: Context
    ): FlowSettings {
        return DataStoreSettings(
            PreferenceDataStoreFactory.create {
                context.dataStoreFile("settings.preferences_pb")
            }
        )
    }

    @AppComp
    @Provides
    fun appInfo(): AppInfo = AppInfo(BuildConfig.VERSION_NAME, "Android")

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
        context: Context
    ): GoogleAuthControllerImpl =
        GoogleAuthControllerImpl(context.getString(R.string.default_web_client_id))

    @AppComp
    @Provides
    fun googleAuthRepository(
        impl: GoogleAuthControllerImpl
    ): GoogleAuthController = impl

    @AppComp
    @Provides
    fun databaseFactory(context: Context) = DatabaseDriverFactory(context)

    @AppComp
    @Provides
    fun nlpCore(context: Context, fileSystem: FileSystem): NLPCore {
        val nlpIndexPath = context.filesDir.absolutePath.toPath().div("nlp")
        if (!fileSystem.exists(nlpIndexPath)) {
            fileSystem.createDirectory(nlpIndexPath)
        }
        return NLPCore(
            context.resources,
            R.raw.en_sent,
            R.raw.en_token,
            R.raw.en_pos_maxent,
            R.raw.en_lemmatizer_dict,
            R.raw.en_chunker,
            nlpIndexPath,
            fileSystem
        )
    }

    @AppComp
    @Provides
    fun secureCodec(): SecureCodec {
        val alias = "secureKeyPair"
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        if (!ks.aliases().toList().contains(alias)) {
            val spec = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            spec.initialize(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
                )
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) //  RSA/ECB/PKCS1Padding
                    .setKeySize(2048) // *** Replaced: setStartDate
//                    .setKeyValidityStart(notBefore.getTime()) // *** Replaced: setEndDate
//                    .setKeyValidityEnd(notAfter.getTime()) // *** Replaced: setSubject
                    .setCertificateSubject(X500Principal("CN=test")) // *** Replaced: setSerialNumber
                    .setCertificateSerialNumber(BigInteger.ONE)
//                    .setUserAuthenticationRequired(true)
//                    .apply {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL)
//                        }
//                    }
                    .build()
            )
            spec.generateKeyPair()
        }

        return SecureCodec(ks, alias)
    }

    // Features

    @AppComp
    @Provides
    fun cardSetsFeatures(): CardSetsVM.Features {
        return CardSetsVM.Features(
            canImportCardSetFromJson = false,
        )
    }
}