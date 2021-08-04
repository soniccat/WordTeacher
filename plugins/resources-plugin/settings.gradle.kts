enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        mavenCentral()
        google()

        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()

        jcenter {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

//includeBuild("../configure-plugin")