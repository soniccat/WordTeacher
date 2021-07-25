
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("com.android.tools.build:gradle:7.0.0-beta05")
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.5.10")
//        classpath(com.aglushkov.plugins.deps.Deps.SqlDelight.classpath)
        //classpath("com.squareup.sqldelight:gradle-plugin:1.5.0")
//        classpath(":build-logic")
//        classpath(":resources-plugin")
    }
}

dependencyResolutionManagement {
//    versionCatalogs {
//        create("libs") {
//            from(files("./gradle/libs.versions.toml"))
//        }
//    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}