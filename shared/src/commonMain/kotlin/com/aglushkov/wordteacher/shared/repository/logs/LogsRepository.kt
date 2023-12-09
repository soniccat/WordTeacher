package com.aglushkov.wordteacher.shared.repository.logs

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path

@OptIn(ExperimentalSettingsApi::class)
class LogsRepository(
    private val settings: FlowSettings,
    private val logFolderPath: Path,
    private val fileSystem: FileSystem,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val isLoggingEnabledState = settings.getBooleanFlow(IS_LOGGING_SETTING_NAME, false)
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun logPaths(): List<Path> {
        return fileSystem.list(logFolderPath).filter {
            fileSystem.metadataOrNull(it)?.isRegularFile == true
        }.sortedByDescending { fileSystem.metadataOrNull(it)?.createdAtMillis ?: 0L }
    }

    fun setIsLoggingEnabled(value: Boolean) {
        scope.launch {
            settings.putBoolean(IS_LOGGING_SETTING_NAME, value)
        }
    }
}

private const val IS_LOGGING_SETTING_NAME = "isLoggingEnabled"