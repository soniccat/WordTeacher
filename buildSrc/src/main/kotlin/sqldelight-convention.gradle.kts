plugins {
    id("com.squareup.sqldelight")
}

sqldelight {
    database("SQLDelightDatabase") {
        packageName = "com.aglushkov.wordteacher.shared.cache"
//        schemaOutputDirectory = File("/shared/src/commonMain/kotlin/com/aglushkov/wordteacher/db")
    }
}
