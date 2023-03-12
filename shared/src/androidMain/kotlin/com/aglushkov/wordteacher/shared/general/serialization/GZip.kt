package com.aglushkov.wordteacher.shared.general.serialization

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual class GZip {
    actual fun compress(text: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        return byteArrayOutputStream.use { stream ->
            GZIPOutputStream(stream).bufferedWriter(Charsets.UTF_8)
                .use { writer -> writer.write(text) }
            stream.toByteArray()
        }
    }

    actual fun decompress(byteArray: ByteArray): String {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        byteArrayInputStream.use { stream ->
            GZIPInputStream(stream).bufferedReader(Charsets.UTF_8)
                .use { reader -> return reader.readText() }
        }
    }
}