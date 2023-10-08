package com.aglushkov.wordteacher.desktopapp.general.crypto

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v1CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

class CertCreator(
    val keyType: String = "RSA",
    val keyGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(keyType),
    val rsaKeylength: Int = 2048,
    val certificateSignatureAlgorithm: String = "SHA1WithRSA",
    val secureRandom: SecureRandom = SecureRandom(),
    val provider: String = "BC",
    val issuer: X500Name = X500Name("C=AU, O=WordTeacher, OU=WordTeacher Primary Certificate"),
    val certificateValidityDays: Long = 100 * 365
) {
    var publicKey: PublicKey? = null
        private set
    var privateKey: PrivateKey? = null
        private set

    init {
        generateKeys()
    }

    private fun generateKeys() {
        val pair: KeyPair
        try {
            keyGenerator.initialize(rsaKeylength, secureRandom)
            pair = keyGenerator.generateKeyPair()
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
        publicKey = pair.public
        privateKey = pair.private

        // publicKey's format must be X.509 otherwise
        // the whole CertGen part of this class is broken.
        val safePublicKey = pair.public
        if (!"X.509".equals(safePublicKey.format, ignoreCase = true)) {
            throw IllegalArgumentException("publicKey's is not X.509, but " + safePublicKey.format)
        }
    }

    fun createSelfSignedCert(): X509Certificate {
        val firstDate = Date()
        val lastDate = Date(firstDate.time + certificateValidityDays * 1000)
        val v1Bldr: X509v1CertificateBuilder = JcaX509v1CertificateBuilder(
            issuer,
            BigInteger.valueOf(1),
            firstDate,
            lastDate,
            issuer,
            publicKey
        )
        val certHolder = v1Bldr.build(JcaContentSignerBuilder(certificateSignatureAlgorithm)
            .setProvider(provider)
            .setSecureRandom(SecureRandom())
            .build(privateKey))

        return JcaX509CertificateConverter().setProvider(provider).getCertificate(certHolder)
    }
}