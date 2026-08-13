import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

// Telegram props
var telegramProps: Properties? = null
val telegramPropFile = file("${project.rootDir}/android_app/telegram.properties")
if (telegramPropFile.exists()) {
    telegramProps = Properties().apply {
        load(FileInputStream(telegramPropFile))
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-andorid/")
    maven("https://artifactory-external.vkpartner.ru/artifactory/maven/")
    maven("https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
    maven {
        url = uri("https://maven.pkg.github.com/TelegramMessenger/telegram-login-android")
        credentials {
            username = telegramProps!!["telegram_github_user"]?.toString() ?: providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_USERNAME")
            password = telegramProps!!["telegram_github_key"]?.toString() ?: providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

android {
    namespace = "com.aglushkov.wordteacher.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":android_app"

    flavorDimensions += listOf("auth")
    productFlavors {
        create("rustore") { dimension = "auth" }
        create("full") { dimension = "auth" }
    }

}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    onVariants { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)?.applicationId }
        )
    }
}