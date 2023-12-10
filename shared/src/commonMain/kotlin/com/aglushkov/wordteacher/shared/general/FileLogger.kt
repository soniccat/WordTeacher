package com.aglushkov.wordteacher.shared.general

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import com.aglushkov.wordteacher.shared.general.extensions.writeStringValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

open class FileLogger(
    private val dirPath: Path,
    private val fileSystem: FileSystem,
    private val timeSource: TimeSource,
    private val isEnabledProvider: () -> Boolean,
    private val formatter: MessageStringFormatter = DefaultFormatter,
) : LogWriter() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fileName = timeSource.stringDate(timeSource.timeInstant())
    private val messageSharedFlow = MutableSharedFlow<LogMessage>(0, 10)

    init {
        scope.launch {
            val filePath = dirPath.div(fileName)
            fileSystem.appendingSink(filePath, false).buffer().use { sink ->
                messageSharedFlow.collect { message ->
                    sink.writeUtf8(message.format(formatter))
                    sink.writeUtf8("\n")

                    message.throwable?.stackTraceToString()?.let { str ->
                        sink.writeUtf8(message.format(formatter))
                        sink.writeUtf8("\n")
                    }

                    sink.flush()
                }
            }
        }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        scope.launch {
            if (isEnabledProvider()) {
                messageSharedFlow.emit(
                    LogMessage(severity, message, tag, throwable)
                )
            }
        }
    }

    data class LogMessage(
        var severity: Severity,
        var message: String,
        var tag: String,
        var throwable: Throwable?,
    ) {
        fun format(messageStringFormatter: MessageStringFormatter): String {
            return messageStringFormatter.formatMessage(severity, Tag(tag), Message(message))
        }
    }
}
