enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
//    repositories {
//        mavenCentral()
//        google()
//
//        jcenter {
//            content {
//                includeGroup("org.jetbrains.kotlinx")
//            }
//        }
//    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}