package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(SQLDelightDatabase.Schema, "wt.db")
    }
}