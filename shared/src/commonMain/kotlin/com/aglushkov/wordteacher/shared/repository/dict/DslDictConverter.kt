package com.aglushkov.wordteacher.shared.repository.dict

import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.okio.deleteIfExists
import com.aglushkov.wordteacher.shared.general.okio.useAsTmp
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import io.ktor.utils.io.charsets.Charset
import no.synth.kmpzip.okio.GzipInputStream
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import javax.xml.stream.events.Characters
import kotlin.io.path.extension

class DslDictConverter(
    val fileSystem: FileSystem
): FileOpenController.Converter {
    private val gzipExtensions = setOf("gz", "gzip", "dz")

    override fun convert(path: Path): String? {
        var newName: String? = null
        val parent = path.parent ?: throw RuntimeException("Source: path parent isn't found")

        // unzip if required
        val fileExtension = path.name.substringAfterLast('.', "")
        if (fileExtension.isNotEmpty() && gzipExtensions.contains(fileExtension)) {
            parent.div(path.name + "_unzipped").useAsTmp { unzippedPath ->
                GzipInputStream(path.toFile().source().buffer())
                    .source().buffer().writeTo(unzippedPath.toFile().sink())
                path.deleteIfExists()
                fileSystem.atomicMove(unzippedPath, path)
            }

            newName = path.name.substring(0, path.name.length - fileExtension.length - 1)
        }

        // check if encoding is utf-16
        var fileCharset: Charset? = Charsets.UTF_8
        path.toFile().source().buffer().use { source ->
            val readByteArray = ByteArray(8 * 1024)
            val readByteCount = source.read(readByteArray, 0, readByteArray.size)
            if (readByteCount >= 2) { // first line, BOM check
                if (readByteArray[0] == 0xFE.toByte() && readByteArray[1] == 0xFF.toByte()) {
                    fileCharset = Charsets.UTF_16BE
                } else if(readByteArray[0] == 0xFF.toByte() && readByteArray[1] == 0xFE.toByte()) {
                    fileCharset = Charsets.UTF_16LE
                } else {
                    val zeroByteCount = readByteArray.count { it == 0x00.toByte() }
                    val zeroBytePercent = zeroByteCount.toFloat() / readByteArray.size.toFloat()
                    if (zeroBytePercent > 0.3f) {
                        fileCharset = Charsets.UTF_16LE
                    }
                }
            }
        }

        if (fileCharset != null && fileCharset != Charsets.UTF_8) {
            // convert file into utf-8
            parent.div(path.name + "_utf8").useAsTmp { utf8Path ->
                utf8Path.toFile().sink().buffer().use { sink ->
                    path.toFile().source().buffer().use {
                        val readByteArray = ByteArray(10 * 1024)
                        var readByteCount: Int
                        while(true) {
                            readByteCount = it.read(readByteArray,0,readByteArray.size)
                            if (readByteCount == -1) {
                                break
                            }

                            sink.writeUtf8(String(readByteArray, fileCharset))
                        }
                    }
                }

                path.deleteIfExists()
                fileSystem.atomicMove(utf8Path, path)
            }
        }

        return newName
    }
}