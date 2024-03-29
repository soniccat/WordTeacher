plugins {
    id("app.cash.sqldelight")
}

sqldelight {
    databases {
        create("MainDB") {
            packageName.set("com.aglushkov.wordteacher.maindb")
            schemaOutputDirectory.set(File("./src/commonMain/sqldelight/main/com/aglushkov/wordteacher/schemes"))
            srcDirs.setFrom("src/commonMain/sqldelight/main")
        }
        create("WordFrequencyDB") {
            packageName.set("com.aglushkov.wordteacher.wordfrequencydb")
            schemaOutputDirectory.set(File("./src/commonMain/sqldelight/wordfrequency/com/aglushkov/wordteacher/schemes"))
            srcDirs.setFrom("src/commonMain/sqldelight/wordfrequency")
        }
    }
//    database("SQLDelightDatabase") {
//        packageName = "com.aglushkov.wordteacher.shared.data"
//        schemaOutputDirectory = File("./src/commonMain/sqldelight/data/com/aglushkov/wordteacher/schemes")
//        sourceFolders = listOf("sqldelight", "data")
//    }
}
