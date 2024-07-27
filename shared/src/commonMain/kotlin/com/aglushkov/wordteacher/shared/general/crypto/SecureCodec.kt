package com.aglushkov.wordteacher.shared.general.crypto

expect class SecureCodec {
    fun encrypt(value: ByteArray): ByteArray
    fun decrypt(value: ByteArray): ByteArray?
}

const val SECURE_CODEC_ALIAS_NAME = "WordTeacher"