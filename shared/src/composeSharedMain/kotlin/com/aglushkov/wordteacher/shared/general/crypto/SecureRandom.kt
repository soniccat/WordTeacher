package com.aglushkov.wordteacher.shared.general.crypto

private val secureRandom = java.security.SecureRandom()

actual class SecureRandom {
    actual fun nextBytes(bytes: ByteArray) {
        secureRandom.nextBytes(bytes)
    }
}