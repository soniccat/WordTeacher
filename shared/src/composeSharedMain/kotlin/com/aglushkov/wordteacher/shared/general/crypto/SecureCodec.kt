package com.aglushkov.wordteacher.shared.general.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

actual class SecureCodec(
    private val ks: KeyStore, // an already loaded one
    private val keyAlias: String,
) {
    actual fun encript(value: ByteArray): ByteArray {
        val key = getPublicKey()
        val encryptCypher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        encryptCypher.init(Cipher.ENCRYPT_MODE, key)

        val byteArrayStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(byteArrayStream, encryptCypher)
        cipherOutputStream.write(value)
        cipherOutputStream.close()

        return byteArrayStream.toByteArray()
    }

    actual fun decript(value: ByteArray): ByteArray? {
        val key = getPrivateKey()
        val encryptCypher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        encryptCypher.init(Cipher.DECRYPT_MODE, key)

        val cipherInputStream = CipherInputStream(ByteArrayInputStream(value), encryptCypher)
        var nextByte: Int
        val decriptedByteArray = mutableListOf<Byte>()
        while (true) {
            nextByte = cipherInputStream.read()
            if (nextByte != -1) {
                decriptedByteArray.add(nextByte.toByte())
            } else {
                break
            }
        }
        cipherInputStream.close()

        return decriptedByteArray.toByteArray()
    }

    private fun getPublicKey(): PublicKey {
        val key = ks.getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry
        return key.certificate.publicKey
    }

    private fun getPrivateKey(): PrivateKey {
        val key = ks.getEntry(keyAlias, null) as KeyStore.PrivateKeyEntry
        return key.privateKey
    }
}
