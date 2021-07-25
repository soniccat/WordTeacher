
rootProject.name = "WordTeacher"

includeBuild("build-logic")
includeBuild("build-logic/deps")
//includeBuild("build-logic2")
includeBuild("resources-plugin")
include(":androidApp")
include(":shared")

//dependencyResolutionManagement {
//    versionCatalogs {
//        create("libs") {
//            from(files("./gradle/libs.versions.toml"))
//        }
//    }
//
//    repositories {
//        google()
//        mavenCentral()
//        gradlePluginPortal()
//        mavenLocal()
//    }
//}
