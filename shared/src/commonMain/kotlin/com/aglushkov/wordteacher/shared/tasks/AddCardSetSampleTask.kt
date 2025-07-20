package com.aglushkov.wordteacher.shared.tasks

import androidx.datastore.preferences.core.booleanPreferencesKey
import co.touchlab.kermit.Logger
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.ImportCardSet
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json

class AddCardSetSampleTask(
    private val settings: SettingStore,
    private val cardSetsRepository: CardSetsRepository,
    private val timeSource: TimeSource,
    private val jsonProvider: () -> String
): Task {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    override suspend fun run(nextTasksChannel: Channel<Task>) {
        val isImported = settings.boolean(IS_CARDSET_SAMPLE_IMPORTED_KEY) ?: false
        if (isImported) {
            return
        }

        val jsonText = jsonProvider()
        val importCardSet = json.decodeFromString<ImportCardSet>(jsonText)
        val nowTime = timeSource.timeInstant()
        cardSetsRepository.insertCardSet(importCardSet.toCardSet(nowTime))

        settings[IS_CARDSET_SAMPLE_IMPORTED_KEY] = true
    }
}

private const val IS_CARDSET_SAMPLE_IMPORTED_KEY = "IS_CARDSET_SAMPLE_IMPORTED_KEY"
