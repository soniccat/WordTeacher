package com.aglushkov.wordteacher.shared.repository.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.aglushkov.wordteacher.maindb.MainDB
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createMainDBDriver(): SqlDriver {
        return AndroidSqliteDriver(
            MainDB.Schema,
            context,
            "test.db",
            callback = AndroidSqliteDriver.Callback(
                schema = MainDB.Schema,
//                AfterVersionWithDriver(1) {
//
//                    Logger.v("migrated to 2")
//                }
            )
        )
    }

    actual fun createFrequencyDBDriver(): SqlDriver =
        createFrequencyDBDriverByName(FREQUENCY_DB_NAME)

    actual fun createTestFrequencyDBDriver(): SqlDriver =
        createFrequencyDBDriverByName(FREQUENCY_DB_NAME_TMP)

    private fun createFrequencyDBDriverByName(name: String): SqlDriver {
        return AndroidSqliteDriver(
            WordFrequencyDB.Schema,
            context,
            name,
            callback = AndroidSqliteDriver.Callback(
                schema = WordFrequencyDB.Schema,
            )
        )
    }
}
