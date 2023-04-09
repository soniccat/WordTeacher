plugins {
    id("android-app-convention")
    id("android-base-convention")
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

val appVersionName = property("versionName")!!.toString()
val appVersionCode = property("versionCode")!!.toString().toInt()

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    // to use SNAPSHOT versions of compose (https://androidx.dev/snapshots/builds)
//    maven(url = "https://androidx.dev/snapshots/builds/8003490/artifacts/repository")

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}


android {
    defaultConfig {
        applicationId = "com.aglushkov.wordteacher"
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xinline-classes" + "-P" + "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=/Users/aoglushkov/androidProjects/WordTeacher/composedebug"
    }

    composeOptions {
        kotlinCompilerVersion = libs.versions.kotlinVersion.get()
        // for compose-jb - comment - start
        kotlinCompilerExtensionVersion = libs.versions.composeJetpackCompiler.get()
        // for compose-jb - comment - end
    }

    buildTypes {
        getByName("debug") {

        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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
    implementation(libs.okio)
    implementation(libs.settingsDataStore) {
        // as we use mt coroutines
        exclude(group = "com.russhwolf", module = "multiplatform-settings-coroutines-android-debug")
    }
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation(libs.dagger)
    kapt(libs.daggerCompiler)

    implementation(libs.playServicesAuth)
}