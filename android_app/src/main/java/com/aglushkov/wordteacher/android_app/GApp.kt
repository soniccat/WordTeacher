package com.aglushkov.wordteacher.android_app

import android.app.Application
import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.android_app.di.AppComponentOwner
import com.aglushkov.wordteacher.android_app.di.DaggerAppComponent
import com.aglushkov.wordteacher.android_app.di.GeneralModule
import com.aglushkov.wordteacher.android_app.di.GeneralModule_ContextFactory.context
import com.aglushkov.wordteacher.android_app.general.ActivityVisibilityResolver
import com.aglushkov.wordteacher.android_app.general.RouterResolver
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.workers.DatabaseCardWorker
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.inject.Inject
import javax.security.auth.x500.X500Principal


class GApp: Application(), AppComponentOwner, ActivityVisibilityResolver.Listener {
    override lateinit var appComponent: AppComponent
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject lateinit var nlpCore: NLPCore
    @Inject lateinit var routerResolver: RouterResolver
    @Inject lateinit var activityVisibilityResolver: ActivityVisibilityResolver

    // declare here to force initialization on startup
    @Inject lateinit var databaseCardWorker: DatabaseCardWorker
    @Inject lateinit var cookieStorage: CookiesStorage

    override fun onCreate() {
        super.onCreate()

        Logger().setupDebug()

        appComponent = DaggerAppComponent.builder()
            .generalModule(GeneralModule(this))
            .build()
        appComponent.injectApplication(this)

        routerResolver.attach()
        activityVisibilityResolver.listener = this
        activityVisibilityResolver.attach()

        appComponent.connectivityManager().checkNetworkState()

        mainScope.launch(Dispatchers.Default) {
            nlpCore.load()
        }

//        val secretKeyGenerator = KeyGenerator.getInstance("AES")
//        val secretKey = secretKeyGenerator.generateKey()

//        val ks = KeyStore.getInstance("AndroidKeyStore")
//        ks.load(null)
//
//        val myKM = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
//        val isDeviceSecure = myKM.isDeviceSecure
//
//        if (!ks.aliases().toList().contains("sss2")) {
//            val spec = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
//            spec.initialize(
//                KeyGenParameterSpec.Builder(
//                    "sss2", KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
//                )
//                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) //  RSA/ECB/PKCS1Padding
//                    .setKeySize(2048) // *** Replaced: setStartDate
////                    .setKeyValidityStart(notBefore.getTime()) // *** Replaced: setEndDate
////                    .setKeyValidityEnd(notAfter.getTime()) // *** Replaced: setSubject
//                    .setCertificateSubject(X500Principal("CN=test")) // *** Replaced: setSerialNumber
//                    .setCertificateSerialNumber(BigInteger.ONE)
//                    .setUserAuthenticationRequired(true).apply {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL)
//                        }
//                    }
//                    .build()
//            )
//            val keyPair: KeyPair = spec.generateKeyPair()
//            Logger.v("generated " + keyPair.public + " " + keyPair.private)
//        }
//
//        if (ks.aliases().toList().isEmpty()) {
////            val secretEntry = KeyStore.SecretKeyEntry(secretKey)
////            ks.setEntry("sss", secretEntry, null)
//
//            //ks.store()
//        } else {
//            Logger.v(ks.aliases().toList().joinToString())
//            val kkk = ks.getEntry("sss2", null) as KeyStore.PrivateKeyEntry
//
//            val encryptCypher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
//            encryptCypher.init(Cipher.ENCRYPT_MODE, kkk.certificate.publicKey)
//
//            val cipherOutputStream = CipherOutputStream(
//                FileOutputStream(encryptedDataFilePath), encryptCypher
//            )
//            // *** Replaced string literal with StandardCharsets.UTF_8
//            cipherOutputStream.write(plainText.getBytes(StandardCharsets.UTF_8))
//            cipherOutputStream.close()
//        }
    }

    override fun onFirstActivityStarted() {
        appComponent.connectivityManager().register()
    }

    override fun onLastActivityStopped() {
        appComponent.connectivityManager().unregister()
    }
}