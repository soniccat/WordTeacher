package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.db.SqlDriver


expect class DatabaseDriverFactory {
    fun createMainDBDriver(): SqlDriver
    fun createFrequencyDBDriver(): SqlDriver
}
