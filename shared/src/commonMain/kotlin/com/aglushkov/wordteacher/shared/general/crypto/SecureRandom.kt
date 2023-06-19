package com.aglushkov.wordteacher.shared.general.crypto

expect class SecureRandom() {
    fun nextBytes(bytes: ByteArray)
}