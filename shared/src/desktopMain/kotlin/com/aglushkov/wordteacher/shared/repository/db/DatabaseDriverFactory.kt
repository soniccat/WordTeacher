package com.aglushkov.wordteacher.shared.repository.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.dir"), "test.db")
        return JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
    }
}
