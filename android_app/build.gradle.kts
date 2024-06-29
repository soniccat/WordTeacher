import java.io.FileInputStream
import java.util.Properties

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
    maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-andorid/")
}

// VK props
var vkProps: Properties? = null
val vkPropFile = file("${project.rootDir}/android_app/vk.properties")
if (vkPropFile.exists()) {
    vkProps = Properties().apply {
        load(FileInputStream(vkPropFile))
    }
}

// App Metrica props
var yandexProps: Properties? = null
val yandexPropFile = file("${project.rootDir}/android_app/yandex.properties")
if (yandexPropFile.exists()) {
    yandexProps = Properties().apply {
        load(FileInputStream(yandexPropFile))
    }
}

android {
    defaultConfig {
        applicationId = "com.aglushkov.wordteacher"
        versionCode = appVersionCode
        versionName = appVersionName

        addManifestPlaceholders(
            buildMap {
                vkProps?.onEach {
                    put(it.key.toString(), it.value)
                }
            }
        )
    }

    namespace = "com.aglushkov.wordteacher.android_app"

    buildFeatures {
        viewBinding = true
        compose = true
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        //freeCompilerArgs = freeCompilerArgs + "-Xinline-classes" + "-P" + "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=/Users/aoglushkov/androidProjects/WordTeacher/composedebug"
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    composeOptions {
        //kotlinCompilerVersion = libs.versions.kotlinVersion.get()
        // for compose-jb - comment - start
        kotlinCompilerExtensionVersion = libs.versions.composeJetpackCompiler.get()
        // for compose-jb - comment - end
    }

    buildTypes {
        defaultConfig {
            yandexProps?.onEach {
                resValue("string", it.key.toString(), it.value.toString())
            }
        }

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
    implementation(libs.decomposeJetbrains)
    implementation(libs.essentyParcelable)
    implementation(libs.androidComposeUITooling)
    implementation(libs.androidComposeUIToolingPreview)
    runtimeOnly("androidx.compose.ui:ui:1.5.2")

    implementation("com.google.accompanist:accompanist-insets:0.17.0")

    // for compose-jb - comment - start
//    implementation(libs.androidComposeFoundation)
//    implementation(libs.androidComposeMaterial)
//    implementation(libs.androidComposeCompiler)
    // for compose-jb - comment - end
    // for compose-jb - uncomment - start
    implementation(compose.material)
    // for compose-jb - uncomment - end

    implementation(libs.coroutinesCommon)
    implementation(libs.coroutinesAndroid)
    implementation(libs.ktorAndroidClient)
//    implementation(libs.okio)
    implementation(libs.settingsDataStore)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation(libs.dagger)
    kapt(libs.daggerCompiler)

    implementation(libs.playServicesAuth)
    implementation(libs.vkId)
    implementation(libs.appmetrica)

    kapt(libs.daggerCompiler)
}

kotlin {
    jvmToolchain(17)
}