package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aglushkov.wordteacher.maindb.MainDB
import java.io.File

actual class DatabaseDriverFactory(val path: String) {
    actual fun createMainDBDriver(): SqlDriver {
        val databasePath = File(path, "test.db")
        return JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
    }

    actual fun createFrequencyDBDriver(): SqlDriver {
        val databasePath = File(path, "word_frequency.db")
        return JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
    }
}
