package com.aglushkov.wordteacher.shared.general.extensions

import okio.BufferedSink


fun BufferedSink.writeIntValue(key: Int, value: Int) {
    writeInt(key)
    writeUtf8(" ")
    writeInt(value)
    writeUtf8("\n")
}

fun BufferedSink.writeIntValue(key: Byte, value: Int) {
    writeByte(key.toInt())
    writeUtf8(" ")
    writeInt(value)
    writeUtf8("\n")
}

fun BufferedSink.writeLongValue(key: Int, value: Long) {
    writeInt(key)
    writeUtf8(" ")
    writeLong(value)
    writeUtf8("\n")
}

fun BufferedSink.writeLongValue(key: Byte, value: Long) {
    writeByte(key.toInt())
    writeUtf8(" ")
    writeLong(value)
    writeUtf8("\n")
}

fun BufferedSink.writeStringValue(key: Int, value: String) {
    writeInt(key)
    writeUtf8(" ")
    writeUtf8(value)
    writeUtf8("\n")
}

fun BufferedSink.writeStringValue(key: Byte, value: String) {
    writeByte(key.toInt())
    writeUtf8(" ")
    writeUtf8(value)
    writeUtf8("\n")
}