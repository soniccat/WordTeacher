enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

//includeBuild("plugins/configure-plugin")
//includeBuild("plugins/resources-plugin")
include(":androidApp")
include(":shared")
include(":desktopApp")
