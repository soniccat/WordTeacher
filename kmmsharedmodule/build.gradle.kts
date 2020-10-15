plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("kotlin-android-extensions")
    kotlin("native.cocoapods")
}

repositories {
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

version = "1.0"

kotlin {
    android()
    ios()
//    ios {
//        binaries {
//            framework {
//                baseName = "kmmsharedmodule2"
//            }
//        }
//    }
    sourceSets {
        val commonMain by getting
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.2.0")
            }
        }
        val iosMain by getting
    }
    cocoapods {
        summary = "Kotlin sample project with CocoaPods dependencies"
        homepage = "https://github.com/Kotlin/kotlin-with-cocoapods-sample"

        ios.deploymentTarget = "13.5"

//      Example of usage remote Cocoapods dependency from Cocoapods repository
        pod("AFNetworking","4.0.1")
    }
}
android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}