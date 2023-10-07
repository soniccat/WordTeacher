//@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.aglushkov.wordteacher.desktopapp.general.crypto

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v1CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Date


// source: https://alvinalexander.com/java/jwarehouse/openjdk-8/jdk/src/share/classes/sun/security/tools/keytool/CertAndKeyGen.java.shtml

/**
 * Generate a pair of keys, and provide access to them.  This class is
 * provided primarily for ease of use.
 *
 * <P>This provides some simple certificate management functionality.
 * Specifically, it allows you to create self-signed X.509 certificates
 * as well as PKCS 10 based certificate signing requests.
 *
</P> * <P>Keys for some public key signature algorithms have algorithm
 * parameters, such as DSS/DSA.  Some sites' Certificate Authorities
 * adopt fixed algorithm parameters, which speeds up some operations
 * including key generation and signing.  *At this time, this interface
 * does not provide a way to provide such algorithm parameters, e.g.
 * by providing the CA certificate which includes those parameters.*
 *
</P> * <P>Also, note that at this time only signature-capable keys may be
 * acquired through this interface.  Diffie-Hellman keys, used for secure
 * key exchange, may be supported later.
 *
 * @author David Brownell
 * @author Hemma Prafullchandra
 * @see PKCS10
 *
 * @see X509CertImpl
</P> */
class CertAndKeyGen {
    /**
     * Creates a CertAndKeyGen object for a particular key type
     * and signature algorithm.
     *
     * @param keyType type of key, e.g. "RSA", "DSA"
     * @param sigAlg name of the signature algorithm, e.g. "MD5WithRSA",
     * "MD2WithRSA", "SHAwithDSA".
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     */
    constructor(keyType: String?, sigAlg: String) {
        keyGen = KeyPairGenerator.getInstance(keyType)
        this.sigAlg = sigAlg
    }

    /**
     * Creates a CertAndKeyGen object for a particular key type,
     * signature algorithm, and provider.
     *
     * @param keyType type of key, e.g. "RSA", "DSA"
     * @param sigAlg name of the signature algorithm, e.g. "MD5WithRSA",
     * "MD2WithRSA", "SHAwithDSA".
     * @param providerName name of the provider
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
     */
    constructor(keyType: String?, sigAlg: String, providerName: String?) {
        if (providerName == null) {
            keyGen = KeyPairGenerator.getInstance(keyType)
        } else {
            try {
                keyGen = KeyPairGenerator.getInstance(keyType, providerName)
            } catch (e: Exception) {
                // try first available provider instead
                keyGen = KeyPairGenerator.getInstance(keyType)
            }
        }
        this.sigAlg = sigAlg
    }

    /**
     * Sets the source of random numbers used when generating keys.
     * If you do not provide one, a system default facility is used.
     * You may wish to provide your own source of random numbers
     * to get a reproducible sequence of keys and signatures, or
     * because you may be able to take advantage of strong sources
     * of randomness/entropy in your environment.
     */
    fun setRandom(generator: SecureRandom?) {
        prng = generator
    }
    // want "public void generate (X509Certificate)" ... inherit DSA/D-H param
    /**
     * Generates a random public/private key pair, with a given key
     * size.  Different algorithms provide different degrees of security
     * for the same key size, because of the "work factor" involved in
     * brute force attacks.  As computers become faster, it becomes
     * easier to perform such attacks.  Small keys are to be avoided.
     *
     * <P>Note that not all values of "keyBits" are valid for all
     * algorithms, and not all public key algorithms are currently
     * supported for use in X.509 certificates.  If the algorithm
     * you specified does not produce X.509 compatible keys, an
     * invalid key exception is thrown.
     *
     * @param keyBits the number of bits in the keys.
     * @exception InvalidKeyException if the environment does not
     * provide X.509 public keys for this signature algorithm.
    </P> */
    @Throws(InvalidKeyException::class)
    fun generate(keyBits: Int) {
        val pair: KeyPair
        try {
            if (prng == null) {
                prng = SecureRandom()
            }
            keyGen!!.initialize(keyBits, prng)
            pair = keyGen!!.generateKeyPair()
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
        publicKeyAnyway = pair.public
        privateKey = pair.private

        // publicKey's format must be X.509 otherwise
        // the whole CertGen part of this class is broken.
        val safePublicKey = pair.public
        if (!"X.509".equals(safePublicKey.format, ignoreCase = true)) {
            throw IllegalArgumentException("publicKey's is not X.509, but " + safePublicKey.format)
        }
    }

    /**
     * Returns the public key of the generated key pair if it is of type
     * `X509Key, or null if the public key is of a different type.
     *
     * XXX Note: This behaviour is needed for backwards compatibility.
     * What this method really should return is the public key of the
     * generated key pair, regardless of whether or not it is an instance of
     * `X509Key. Accordingly, the return type of this method
     * should be `PublicKey.
    ``` */
//    fun getPublicKey(): X509Key? {
//        return if (!(publicKeyAnyway is X509Key)) {
//            null
//        } else publicKeyAnyway as X509Key?
//    }

    /**
     * Returns a self-signed X.509v3 certificate for the public key.
     * The certificate is immediately valid. No extensions.
     *
     * <P>Such certificates normally are used to identify a "Certificate
     * Authority" (CA).  Accordingly, they will not always be accepted by
     * other parties.  However, such certificates are also useful when
     * you are bootstrapping your security infrastructure, or deploying
     * system prototypes.
     *
     * @param myname X.500 name of the subject (who is also the issuer)
     * @param firstDate the issue time of the certificate
     * @param validity how long the certificate should be valid, in seconds
     * @exception CertificateException on certificate handling errors.
     * @exception InvalidKeyException on key handling errors.
     * @exception SignatureException on signature handling errors.
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
    </P> */
    @Throws(
        CertificateException::class,
        InvalidKeyException::class,
        SignatureException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class
    )
    fun getSelfCertificate(
        myname: X500Name?, firstDate: Date, validity: Long
    ): X509Certificate {
        return createAcIssuerCert(publicKeyAnyway, privateKey)!! //getSelfCertificate(myname, firstDate, validity, null)
    }

    /**
     * we generate the AC issuer's certificate
     */
    @Throws(java.lang.Exception::class)
    fun createAcIssuerCert(
        pubKey: PublicKey?,
        privKey: PrivateKey?
    ): X509Certificate? {
        //
        // signers name
        //
        val issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate"

        //
        // subjects name - the same as we are self signed.
        //
        val subject = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate"

        //
        // create the certificate - version 1
        //
        val v1Bldr: X509v1CertificateBuilder = JcaX509v1CertificateBuilder(
            X500Name(issuer), BigInteger.valueOf(1),
            Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), Date(
                System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30
            ),
            X500Name(subject), pubKey
        )
        val certHldr =
            v1Bldr.build(JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(privKey))
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr)
        cert.checkValidity(Date())
        cert.verify(pubKey)
        return cert
    }

    // Like above, plus a CertificateExtensions argument, which can be null.
//    @Throws(
//        CertificateException::class,
//        InvalidKeyException::class,
//        SignatureException::class,
//        NoSuchAlgorithmException::class,
//        NoSuchProviderException::class
//    )
//    fun getSelfCertificate(
//        myname: X500Name?,
//        firstDate: Date,
//        validity: Long,
//        ext: CertificateExtensions?
//    ): X509Certificate {
//        val b = BcX509v3CertificateBuilder()
//
//        val cert: X509CertImpl
//        val lastDate: Date
//        try {
//            lastDate = Date()
//            lastDate.setTime(firstDate.getTime() + validity * 1000)
//            val interval = CertificateValidity(firstDate, lastDate)
//            val info = X509CertInfo()
//            // Add all mandatory attributes
//            info[X509CertInfo.VERSION] = CertificateVersion(CertificateVersion.V3)
//            info[X509CertInfo.SERIAL_NUMBER] = CertificateSerialNumber(
//                Random().nextInt() and 0x7fffffff
//            )
//            val algID = AlgorithmId.get(sigAlg)
//            info[X509CertInfo.ALGORITHM_ID] = CertificateAlgorithmId(algID)
//            info[X509CertInfo.SUBJECT] = myname
//            info[X509CertInfo.KEY] = CertificateX509Key(
//                publicKeyAnyway
//            )
//            info[X509CertInfo.VALIDITY] = interval
//            info[X509CertInfo.ISSUER] = myname
//            if (ext != null) info[X509CertInfo.EXTENSIONS] = ext
//            cert = X509CertImpl(info)
//            cert.sign(privateKey, sigAlg)
//            return cert as X509Certificate
//        } catch (e: IOException) {
//            throw CertificateEncodingException(
//                "getSelfCert: " +
//                        e.message
//            )
//        }
//    }

    // Keep the old method
    @Throws(
        CertificateException::class,
        InvalidKeyException::class,
        SignatureException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class
    )
    fun getSelfCertificate(myname: X500Name?, validity: Long): X509Certificate {
        return getSelfCertificate(myname, Date(), validity)
    }

    /**
     * Returns a PKCS #10 certificate request.  The caller uses either
     * `PKCS10.print or PKCS10.toByteArray
     * operations on the result, to get the request in an appropriate
     * transmission format.
     *
     * <P>PKCS #10 certificate requests are sent, along with some proof
     * of identity, to Certificate Authorities (CAs) which then issue
     * X.509 public key certificates.
     *
     * @param myname X.500 name of the subject
     * @exception InvalidKeyException on key handling errors.
     * @exception SignatureException on signature handling errors.
    </P>` */
//    @Throws(InvalidKeyException::class, SignatureException::class)
//    fun getCertRequest(myname: X500Name?): PKCS10 {
//        val req = PKCS10(publicKeyAnyway)
//        try {
//            val signature: Signature = Signature.getInstance(sigAlg)
//            signature.initSign(privateKey)
//            req.encodeAndSign(myname, signature)
//        } catch (e: CertificateException) {
//            throw SignatureException("$sigAlg CertificateException")
//        } catch (e: IOException) {
//            throw SignatureException("$sigAlg IOException")
//        } catch (e: NoSuchAlgorithmException) {
//            // "can't happen"
//            throw SignatureException("$sigAlg unavailable?")
//        }
//        return req
//    }

    private var prng: SecureRandom? = null
    private var sigAlg: String
    private var keyGen: KeyPairGenerator? = null

    /**
     * Always returns the public key of the generated key pair. Used
     * by KeyTool only.
     *
     * The publicKey is not necessarily to be an instance of
     * X509Key in some JCA/JCE providers, for example SunPKCS11.
     */
    var publicKeyAnyway: PublicKey? = null
        private set

    /**
     * Returns the private key of the generated key pair.
     *
     * <P>Be extremely careful when handling private keys.
     * When private keys are not kept secret, they lose their ability
     * to securely authenticate specific entities ... that is a huge
     * security risk!
    </P> */
    var privateKey: PrivateKey? = null
        private set
}