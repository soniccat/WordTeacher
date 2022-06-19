package com.aglushkov.wordteacher.shared.repository.db

import android.content.Context
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase.Companion.Schema
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.v
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.AfterVersionWithDriver
import com.squareup.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            Schema,
            context,
            "test.db",
            callback = AndroidSqliteDriver.Callback(
                schema = Schema,
                AfterVersionWithDriver(1) {
                    Logger.v("migrated to 2")
                }
            )
        )
    }
}
