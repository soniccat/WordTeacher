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
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
    maven(url = "https://jitpack.io")
}

android {
    compileSdkVersion(Versions.compileSdk)

    defaultConfig {
        applicationId = "com.aglushkov.wordteacher.androidApp"
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xinline-classes"
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
    implementation("com.github.leinardi:FloatingActionButtonSpeedDial:3.1.1")

    implementation(Deps.Coroutines.common)
    implementation(Deps.Coroutines.android)

    implementation(Deps.MokoResources.impl)
    implementation(Deps.mokoMvvmCore)

    implementation(Deps.Ktor.androidClient)

    implementation("com.google.dagger:dagger:2.27")
    kapt("com.google.dagger:dagger-compiler:2.27")
}