package com.aglushkov.wordteacher.shared.repository.logs

import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path

class LogsRepository(
    private val settings: SettingStore,
    private val logFolderPath: Path,
    private val fileSystem: FileSystem,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val isLoggingEnabledState = settings.booleanFlow(IS_LOGGING_SETTING_NAME, false)
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun logPaths(): List<Path> {
        return fileSystem.list(logFolderPath).filter {
            fileSystem.metadataOrNull(it)?.isRegularFile == true
        }.sortedByDescending { fileSystem.metadataOrNull(it)?.createdAtMillis ?: 0L }
    }

    fun setIsLoggingEnabled(value: Boolean) {
        settings[IS_LOGGING_SETTING_NAME] = value
    }
}

private const val IS_LOGGING_SETTING_NAME = "isLoggingEnabled"