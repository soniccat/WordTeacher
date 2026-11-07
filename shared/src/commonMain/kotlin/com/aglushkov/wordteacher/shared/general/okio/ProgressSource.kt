package com.aglushkov.wordteacher.shared.general.okio

import okio.Buffer
import okio.Source

class ProgressSource(
    private val delegate: Source,
    private val progressStep: Long = 1024 * 1024,
    private val onProgress: (bytesRead: Long, isDone: Boolean) -> Unit
) : Source {
    private var totalBytesRead = 0L
    private var lastProgressBytesRead = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = delegate.read(sink, byteCount)
        if (bytesRead != -1L) {
            totalBytesRead += bytesRead
        }

        val isDone = bytesRead == -1L
        if (totalBytesRead - lastProgressBytesRead >= progressStep || isDone) {
            onProgress(totalBytesRead, isDone)
            lastProgressBytesRead = totalBytesRead
        }
        return bytesRead
    }

    override fun timeout() = delegate.timeout()
    override fun close() = delegate.close()
}

fun Source.withProgress(
    onProgress: (bytesRead: Long, isDone: Boolean) -> Unit
): ProgressSource {
    return ProgressSource(this, onProgress = onProgress)
}
