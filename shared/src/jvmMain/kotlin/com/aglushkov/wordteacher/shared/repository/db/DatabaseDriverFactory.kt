package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aglushkov.wordteacher.maindb.MainDB
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB
import java.io.File

actual class DatabaseDriverFactory(val path: String) {
    actual fun createMainDBDriver(): SqlDriver {
        val databasePath = File(path, "test.db")
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${databasePath.absolutePath}",
            schema = MainDB.Schema,
        )
    }

    actual fun createFrequencyDBDriver(): SqlDriver =
        createFrequencyDBDriverByName(FREQUENCY_DB_NAME)

    actual fun createTestFrequencyDBDriver(): SqlDriver =
        createFrequencyDBDriverByName(FREQUENCY_DB_NAME_TMP)

    private fun createFrequencyDBDriverByName(name: String): SqlDriver {
        val databasePath = File(path, name)
        return JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
    }
}
