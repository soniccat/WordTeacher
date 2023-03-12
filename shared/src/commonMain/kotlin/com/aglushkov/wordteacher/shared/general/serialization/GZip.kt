package com.aglushkov.wordteacher.shared.general.serialization

expect class GZip() {
    fun compress(text: String): ByteArray
    fun decompress(byteArray: ByteArray): String
}