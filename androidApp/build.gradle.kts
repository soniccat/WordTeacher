plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
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
        viewBinding = true // TODO: consider to remove
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

    implementation(Deps.Google.material)
    implementation(Deps.Google.appcompat)
    implementation(Deps.Google.constraintLayout)
    implementation(Deps.Google.coreKtx)
    implementation(Deps.Google.lifecycleExtensions)

    implementation(Deps.MokoResources.impl)
    implementation(Deps.mokoMvvm)
}