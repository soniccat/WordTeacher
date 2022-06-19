plugins {
    id("com.squareup.sqldelight")
}

sqldelight {
    database("SQLDelightDatabase") {
        packageName = "com.aglushkov.wordteacher.shared.cache"
        schemaOutputDirectory = File("./src/commonMain/sqldelight/com/aglushkov/wordteacher/schemes")
    }
}
