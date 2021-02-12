package com.aglushkov.wordteacher.shared.repository.db

import android.content.Context
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(SQLDelightDatabase.Schema, context, "test.db")
    }
}
