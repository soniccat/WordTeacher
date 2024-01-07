package com.aglushkov.wordteacher.shared.repository.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.aglushkov.wordteacher.maindb.MainDB.Companion.Schema
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            Schema,
            context,
            "test.db",
            callback = AndroidSqliteDriver.Callback(
                schema = Schema,
//                AfterVersionWithDriver(1) {
//
//                    Logger.v("migrated to 2")
//                }
            )
        )
    }
}
