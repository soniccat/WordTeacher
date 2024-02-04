package com.aglushkov.wordteacher.shared.general.okio

import co.touchlab.kermit.Logger
import okio.BufferedSource
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import okio.use
import okio.utf8Size

fun BufferedSource.skipSpace() = skip(spaceSize)
fun BufferedSource.skipNewLine() = skip(newLineSize)

val spaceSize = " ".utf8Size()
val newLineSize = "\n".utf8Size()

fun Source.writeTo(outputSink: Sink) {
    outputSink.buffer().use { sink ->
        this@writeTo.buffer().use { source ->
            val readByteArray = ByteArray(100 * 1024)
            var readByteCount: Int
            var readCount = 0
            while (true) {
                readByteCount = source.read(readByteArray, 0, readByteArray.size)
                if (readByteCount == -1) {
                    break
                }
                sink.write(readByteArray, 0, readByteCount)
                readCount += readByteCount
            }
        }
    }
}

