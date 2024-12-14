package com.aglushkov.wordteacher.shared.tasks

import co.touchlab.kermit.Logger
import com.aglushkov.wordteacher.shared.features.cardset_json_import.vm.ImportCardSet
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json

class AddCardSetSampleTask(
    private val settings: FlowSettings,
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
        val isImported = settings.getBoolean(IS_CARDSET_SAMPLE_IMPORTED_KEY, false)
        if (isImported) {
            return
        }

        try {
            val jsonText = jsonProvider()
            val importCardSet = json.decodeFromString<ImportCardSet>(jsonText)
            val nowTime = timeSource.timeInstant()
            cardSetsRepository.insertCardSet(importCardSet.toCardSet(nowTime))
        } catch (e: Throwable) {
            Logger.e("parse error", e)
        }

        settings.putBoolean(IS_CARDSET_SAMPLE_IMPORTED_KEY, true)
    }
}

private const val IS_CARDSET_SAMPLE_IMPORTED_KEY = "IS_CARDSET_SAMPLE_IMPORTED_KEY"
