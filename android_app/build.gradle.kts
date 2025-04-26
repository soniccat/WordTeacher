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
            implementation(libs.yandexId)
            implementation(libs.playServicesAuth)
            implementation(libs.androidXBrowser)
        }
    }
}

// Signing file
var debugKeystoreProps: Properties? = null
val debugKeystorePropFile = file("${project.rootDir}/android_app/keystore.properties")
if (debugKeystorePropFile.exists()) {
    debugKeystoreProps = Properties().apply {
        load(FileInputStream(debugKeystorePropFile))
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
        versionCode = 7
        versionName = "1.6"

        addManifestPlaceholders(
            buildMap {
                vkProps?.onEach {
                    put(it.key.toString(), it.value)
                }
                put("YANDEX_CLIENT_ID", yandexProps!!["yandex_id_client_id"]!!)
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

            buildConfigField("int", "defaultWordlistVersion", "1")
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
    signingConfigs {
        getByName("debug") {
            debugKeystoreProps?.let { props ->
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
            }
        }
    }
}

dependencies {
    configurations["kapt"](libs.daggerCompiler)
}
