package com.aglushkov.wordteacher.desktopapp.general.crypto

import com.aglushkov.wordteacher.desktopapp.configs.KeyStoreConfig
import com.aglushkov.wordteacher.shared.di.BasePath
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import okio.FileSystem
import okio.Path
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate

class SecureCodecBuilder(
    private val basePath: Path,
    private val fileSystem: FileSystem,
) {
    fun build(): SecureCodec {
        val keystoreType = "PKCS12"
        val keystoreFilename = "keystore.p12"
        val keystorePassword = KeyStoreConfig.password.map { Char(it.toInt()) }.toCharArray()
        val alias = "word_teacher_key"

        val keyStorePath = basePath.div(keystoreFilename)
        if (!fileSystem.exists(keyStorePath)) {
            try {
                val keyStore = KeyStore.getInstance(keystoreType)
                keyStore.load(null, null)
                keyStore.store(
                    FileOutputStream(keyStorePath.toString()),
                    keystorePassword
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        Security.addProvider(BouncyCastleProvider())

        val keyStore = KeyStore.getInstance(keystoreType)!!
        keyStore.load(FileInputStream(keyStorePath.toString()), keystorePassword)
        if (keyStore.aliases().toList().isEmpty()) {
            try {
                val certGenerator = CertCreator()
                val cert: X509Certificate = CertCreator().createSelfSignedCert()
                keyStore.setKeyEntry(alias, certGenerator.privateKey, keystorePassword, arrayOf(cert))
                keyStore.store(
                    FileOutputStream(keyStorePath.toString()),
                    keystorePassword
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                throw ex
            }
        }

        return SecureCodec(keyStore, alias, KeyStore.PasswordProtection(keystorePassword))
    }
}
