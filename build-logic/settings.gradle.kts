
rootProject.name = "build-logic"

dependencyResolutionManagement {
//    versionCatalogs {
//        create("libs") {
//            from(files("../gradle/libs.versions.toml"))
//        }
//    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}