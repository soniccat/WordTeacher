import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id(Deps.SqlDelight.plugin)
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://jitpack.io")

    // to use SNAPSHOT versions of compose (https://androidx.dev/snapshots/builds)
    maven(url = "https://androidx.dev/snapshots/builds/7473952/artifacts/repository")
}

android {
    compileSdkVersion(Versions.compileSdk)
    buildToolsVersion = Versions.buildToolVersion

    defaultConfig {
        applicationId = "com.aglushkov.wordteacher.androidApp"
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        useIR = true
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xinline-classes"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.compose_jetpack_version
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(Deps.Google.fragments)
    implementation(Deps.Google.material)
    implementation(Deps.Google.appcompat)
    implementation(Deps.Google.constraintLayout)
    implementation(Deps.Google.coreKtx)
    implementation(Deps.Google.viewModelKtx)
    implementation(Deps.Google.lifecycleKtx)
    implementation(Deps.floatingActionButtonSpeedDial)
    implementation(Deps.Decompose.jetpack)
    implementation(Deps.Compose.foundation) {
        version {
            strictly("1.0.0-SNAPSHOT")
        }
    }
    implementation(Deps.Compose.material) {
        version {
            strictly("1.0.0-SNAPSHOT")
        }
    }
    implementation(Deps.Compose.activity) {
        version {
            strictly("1.3.0-SNAPSHOT")
        }
    }
    implementation(Deps.Compose.tooling) {
        version {
            strictly("1.0.0-SNAPSHOT")
        }
    }

    implementation(Deps.Coroutines.common)
    implementation(Deps.Coroutines.android)

    implementation(Deps.MokoResources.impl)

    implementation(Deps.Ktor.androidClient)

    implementation("com.google.dagger:dagger:2.35.1")
    kapt("com.google.dagger:dagger-compiler:2.35.1")
}