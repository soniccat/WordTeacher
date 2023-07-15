package com.aglushkov.wordteacher.shared.general.crypto

import io.ktor.util.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*

class PkceGenerator {
    companion object {
        const val CODE_CHALLENGE_METHOD = "S256"
    }

    fun codeChallenge(codeVerifier: String): String {
        val bytes: ByteArray = codeVerifier.toByteArray(Charsets.UTF_8)
        val digest: ByteArray = bytes.sha256()
        return digest.encodeBase64Url()
    }

    fun codeVerifier(): String {
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(128)
        secureRandom.nextBytes(randomBytes)
        val chars = randomBytes.map {
            VERIFIER_ALPHABET[(it.toUInt() % VERIFIER_ALPHABET.size.toUInt()).toInt()]
        }.toByteArray()
        return String(chars, 0, chars.size, Charsets.UTF_8)
    }
}

private val VERIFIER_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toByteArray(Charsets.UTF_8)