enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "build-logic"

include("base-convention")

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