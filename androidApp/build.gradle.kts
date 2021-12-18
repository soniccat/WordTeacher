plugins {
    id("android-app-convention")
    id("android-base-convention")
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    jcenter()
    // to use SNAPSHOT versions of compose (https://androidx.dev/snapshots/builds)
    maven(url = "https://androidx.dev/snapshots/builds/8003490/artifacts/repository")
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
//        useIR = true
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xinline-classes"
    }

    composeOptions {
        kotlinCompilerVersion = libs.versions.kotlinVersion.get()
        // for compose-jb - comment - start
        kotlinCompilerExtensionVersion = libs.versions.composeJetpack.get()
        // for compose-jb - comment - end
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
    implementation(libs.decompose)
    implementation(libs.decomposeJetpack)
    implementation(libs.essentyParcelable)
    implementation(libs.androidComposeActivity)
    implementation(libs.androidComposeUITooling)
    implementation(libs.androidComposeUIToolingPreview)

    implementation("com.google.accompanist:accompanist-insets:0.17.0")

    // for compose-jb - comment - start
    implementation(libs.androidComposeFoundation)
    implementation(libs.androidComposeMaterial)
    implementation(libs.androidComposeCompiler)
    // for compose-jb - comment - end
    // for compose-jb - uncomment - start
//    implementation(compose.material)
    // for compose-jb - uncomment - end

    implementation(libs.coroutinesCommon)
    implementation(libs.coroutinesAndroid)
    implementation(libs.ktorAndroidClient)

    implementation("com.google.dagger:dagger:2.40.5")
    kapt("com.google.dagger:dagger-compiler:2.40.5")
}