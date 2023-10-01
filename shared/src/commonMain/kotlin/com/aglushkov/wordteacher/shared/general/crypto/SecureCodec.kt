package com.aglushkov.wordteacher.shared.general.crypto

expect class SecureCodec {
    fun encript(value: ByteArray): ByteArray
    fun decript(value: ByteArray): ByteArray?
}
