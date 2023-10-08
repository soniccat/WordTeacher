package com.aglushkov.wordteacher.android_app.general.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aglushkov.wordteacher.shared.general.crypto.SECURE_CODEC_ALIAS_NAME
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.security.auth.x500.X500Principal

class SecureCodecBuilder {
    fun build(): SecureCodec {
        val provider = "AndroidKeyStore"
        val ks = KeyStore.getInstance(provider)
        ks.load(null)

        if (!ks.aliases().toList().contains(SECURE_CODEC_ALIAS_NAME)) {
            val spec = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, provider)
            spec.initialize(
                KeyGenParameterSpec.Builder(
                    SECURE_CODEC_ALIAS_NAME,
                    KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
                )
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
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

        return SecureCodec(ks, SECURE_CODEC_ALIAS_NAME, null)
    }
}