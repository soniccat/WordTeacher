import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.playServicesPlugin)
    alias(libs.plugins.kapt)
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-andorid/")
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

val appVersionName = property("versionName")!!.toString()
val appVersionCode = property("versionCode")!!.toString().toInt()


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

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(projects.shared)
            implementation(libs.dagger)
            implementation(libs.settingsDataStore)
            implementation(libs.datastorePreference)
            implementation(libs.vkId)
            implementation(libs.playServicesAuth)
//
        }
//        desktopMain.dependencies {
//            implementation(compose.desktop.currentOs)
//        }
    }
}

android {
    namespace = "com.aglushkov.wordteacher.android_app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/main/res")
    sourceSets["main"].resources.srcDirs("src/main/resources")

    defaultConfig {
        applicationId = "com.aglushkov.wordteacher"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        addManifestPlaceholders(
            buildMap {
                vkProps?.onEach {
                    put(it.key.toString(), it.value)
                }
            }
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        defaultConfig {
            yandexProps?.onEach {
                resValue("string", it.key.toString(), it.value.toString())
            }
        }

        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
        coreLibraryDesugaring(libs.desugar.jdk.libs)
    }
}

dependencies {
    configurations["kapt"](libs.daggerCompiler)
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "org.example.project"
//            packageVersion = "1.0.0"
//        }
//    }
//}

// old
//plugins {
//    id("android-app-convention")
//    id("android-base-convention")
//}
//
//repositories {
//    google()
//    mavenCentral()
//    gradlePluginPortal()
//    // to use SNAPSHOT versions of compose (https://androidx.dev/snapshots/builds)
////    maven(url = "https://androidx.dev/snapshots/builds/8003490/artifacts/repository")
//
//    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-andorid/")
//}
//
//// VK props
//var vkProps: Properties? = null
//val vkPropFile = file("${project.rootDir}/android_app/vk.properties")
//if (vkPropFile.exists()) {
//    vkProps = Properties().apply {
//        load(FileInputStream(vkPropFile))
//    }
//}
//
//// App Metrica props
//var yandexProps: Properties? = null
//val yandexPropFile = file("${project.rootDir}/android_app/yandex.properties")
//if (yandexPropFile.exists()) {
//    yandexProps = Properties().apply {
//        load(FileInputStream(yandexPropFile))
//    }
//}
//
//android {
//    defaultConfig {
//        applicationId = "com.aglushkov.wordteacher"
//        versionCode = appVersionCode
//        versionName = appVersionName
//
////        addManifestPlaceholders(
////            buildMap {
////                vkProps?.onEach {
////                    put(it.key.toString(), it.value)
////                }
////            }
////        )
//    }
//
//    namespace = "com.aglushkov.wordteacher.android_app"
//
//    buildFeatures {
//        viewBinding = true
//        compose = true
//    }
//
//    java {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//
//    kotlinOptions {
//        jvmTarget = "17"
//        //freeCompilerArgs = freeCompilerArgs + "-Xinline-classes" + "-P" + "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=/Users/aoglushkov/androidProjects/WordTeacher/composedebug"
//        compileOptions {
//            sourceCompatibility = JavaVersion.VERSION_17
//            targetCompatibility = JavaVersion.VERSION_17
//        }
//    }
//
//    composeOptions {
//        //kotlinCompilerVersion = libs.versions.kotlinVersion.get()
//        // for compose-jb - comment - start
//        kotlinCompilerExtensionVersion = libs.versions.composeJetpackCompiler.get()
//        // for compose-jb - comment - end
//    }
//
//    buildTypes {
//        defaultConfig {
//            yandexProps?.onEach {
//                resValue("string", it.key.toString(), it.value.toString())
//            }
//        }
//
//        getByName("debug") {
//
//        }
//        getByName("release") {
//            isMinifyEnabled = true
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
//        }
//    }
//
//    testOptions {
//        unitTests {
//            isIncludeAndroidResources = true
//        }
//    }
//}
//
//dependencies {
//    implementation(project(":shared"))
//
//    implementation(libs.sqlDelightRuntime)
//    implementation(libs.androidFragments)
//    implementation(libs.androidMaterial)
//    implementation(libs.androidAppCompat)
//    implementation(libs.androidConstraintLayout)
//    implementation(libs.androidCoreKts)
//    implementation(libs.androidViewModelKtx)
//    implementation(libs.androidLifecycleKtx)
//    implementation(libs.decompose)
//    implementation(libs.decomposeJetbrains)
//    implementation(libs.essentyParcelable)
//    implementation(libs.androidComposeUITooling)
//    implementation(libs.androidComposeUIToolingPreview)
//    runtimeOnly("androidx.compose.ui:ui:1.5.2")
//
//    implementation("com.google.accompanist:accompanist-insets:0.17.0")
//
//    // for compose-jb - comment - start
////    implementation(libs.androidComposeFoundation)
////    implementation(libs.androidComposeMaterial)
////    implementation(libs.androidComposeCompiler)
//    // for compose-jb - comment - end
//    // for compose-jb - uncomment - start
//    implementation(compose.material)
//    // for compose-jb - uncomment - end
//
//    implementation(libs.coroutinesCommon)
//    implementation(libs.coroutinesAndroid)
//    implementation(libs.ktorAndroidClient)
////    implementation(libs.okio)
//    implementation(libs.settingsDataStore)
//    implementation("androidx.datastore:datastore-preferences:1.0.0")
//
//    implementation(libs.dagger)
//    kapt(libs.daggerCompiler)
//
//    implementation(libs.playServicesAuth)
//    implementation(libs.vkId)
//    implementation(libs.appmetrica)
//
//    kapt(libs.daggerCompiler)
//}
//
//kotlin {
//    jvmToolchain(17)
//}