plugins {
    id("android-app-convention")
    id("android-base-convention")
    id("org.jetbrains.compose")
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

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

android {
    defaultConfig {
        applicationId = "com.aglushkov.wordteacher.androidApp"
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
        kotlinCompilerVersion = libs.versions.kotlinVersion.get()
//        kotlinCompilerExtensionVersion = libs.versions.composeJetpack.get()
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

    implementation(libs.sqlDelightRuntime)
    implementation(libs.androidFragments)
    implementation(libs.androidMaterial)
    implementation(libs.androidAppCompat)
    implementation(libs.androidConstraintLayout)
    implementation(libs.androidCoreKts)
    implementation(libs.androidViewModelKtx)
    implementation(libs.androidLifecycleKtx)
    implementation(libs.floatingActionButtonSpeedDial)
    implementation(libs.decomposeJetpack)
    implementation(compose.material)
//    implementation(libs.essentyParcelable)
//    implementation(libs.androidComposeFoundation) {
//        version {
//            strictly(libs.versions.composeJetpack.get())
//        }
//    }
//    implementation(libs.androidComposeMaterial) {
//        version {
//            strictly(libs.versions.composeJetpack.get())
//        }
//    }
    implementation(libs.androidComposeActivity)
    implementation(compose.uiTooling)

    implementation(libs.coroutinesCommon)
    implementation(libs.coroutinesAndroid)
    implementation(libs.ktorAndroidClient)

    implementation("com.google.dagger:dagger:2.35.1")
    kapt("com.google.dagger:dagger-compiler:2.35.1")
}