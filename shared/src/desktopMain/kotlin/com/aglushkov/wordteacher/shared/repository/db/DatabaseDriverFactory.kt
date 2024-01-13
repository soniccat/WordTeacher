package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory(val path: String) {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(path, "test.db")
        return JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
    }
}
