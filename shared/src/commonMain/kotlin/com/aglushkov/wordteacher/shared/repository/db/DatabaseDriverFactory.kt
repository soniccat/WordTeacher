package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.db.SqlDriver


expect class DatabaseDriverFactory {
    fun createMainDBDriver(): SqlDriver
    fun createFrequencyDBDriver(): SqlDriver
    fun createTestFrequencyDBDriver(): SqlDriver
    fun createMisspellingDBDriver(): SqlDriver
    fun createTmpMisspellingDBDriver(): SqlDriver
}

const val FREQUENCY_DB_NAME = "word_frequency.db"
const val FREQUENCY_DB_NAME_TMP = "word_frequency_tmp.db"
const val MISSPELLING_DB_NAME = "misspelling.db"
const val MISSPELLING_DB_NAME_TMP = "misspelling_tmp.db"
