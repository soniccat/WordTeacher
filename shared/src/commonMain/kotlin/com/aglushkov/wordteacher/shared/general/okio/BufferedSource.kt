package com.aglushkov.wordteacher.shared.general.okio

import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.russhwolf.settings.coroutines.FlowSettings
import okio.BufferedSource
import okio.FileSystem
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

fun FileSystem.writeToWithLockFile(
    src: Source,
    outputPath: Path,
    skipIfFileExists: Boolean = true
) {
    val parent = outputPath.parent ?: throw RuntimeException("Source: outputPath parent isn't found")
    val lockPath = parent.div(outputPath.name + "_lock")

    if (skipIfFileExists && !exists(lockPath) && exists(outputPath)) {
        return
    }

    if (exists(lockPath)) {
        delete(lockPath)
    }
    src.writeTo(sink(lockPath))
    atomicMove(lockPath, outputPath)
    delete(lockPath)
}

fun FileSystem.writeToWithVersioning(
    src: Source?,
    outputPath: Path,
    versionKey: String,
    lastVersion: Int,
    settings: SettingStore,
): Boolean {
    val currentVersion = settings.int(versionKey) ?: -1
    return if (!exists(outputPath) || currentVersion != lastVersion) {
        if (src != null) {
            writeToWithLockFile(src, outputPath)
            settings[versionKey] = lastVersion
            true
        } else {
            false
        }
    } else {
        false
    }
}
