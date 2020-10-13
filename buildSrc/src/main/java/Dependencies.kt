import java.net.URI

object Versions {
    val minSdk = 21
    val targetSdk = 29
    val compileSdk = 29

    val kotlin = "1.4.10"
    val androidx_test = "1.2.0"
    val androidx_test_ext = "1.1.1"
    val android_gradle_plugin = "4.0.1"
    val junit = "4.13"
    val sqlDelight = "1.4.1"
    val ktor = "1.4.0"
    val stately = "1.1.0"
    val multiplatformSettings = "0.6.1"
    val coroutines = "1.3.9-native-mt"
    val koin = "3.0.1-alpha-2"
    val serialization = "1.0.0-RC"
    val cocoapodsext = "0.11"
    val kermit = "0.1.8"
    val lifecycle = "2.1.0"
    val karmok = "0.1.8"
    val ktlint_gradle_plugin = "9.2.1"
    val robolectric = "4.3.1"
}

object Deps {
    object Gradle {
        val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    }

    val kotlinxRepo = URI("https://kotlin.bintray.com/kotlinx/")
    val mokoRepo = URI("https://dl.bintray.com/icerockdev/moko")
    val mokoPluginsRepo = URI("https://dl.bintray.com/icerockdev/plugins")

    object MokoResources {
        val plugin = "dev.icerock.mobile.multiplatform-resources"
        val classpath = "dev.icerock.moko:resources-generator:0.13.1"
        val impl = "dev.icerock.moko:resources:0.13.1"
    }

    object MokoParcelize {
        val impl = "dev.icerock.moko:parcelize:0.4.0"
    }

    object MokoGraphics {
        val impl = "dev.icerock.moko:graphics:0.4.0"
    }

    val mokoMvvm = "dev.icerock.moko:mvvm:0.8.0"

    object Mp {
        val serializationPlugin = "org.jetbrains.kotlin.plugin.serialization"
        val serialization = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
        val serializationCore = "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC"
    }

    // TODO: sort by groups
    object Google {
        val appcompat = "androidx.appcompat:appcompat:1.2.0"
        val material = "com.google.android.material:material:1.2.1"
        val coreKtx = "androidx.core:core-ktx:1.2.0"
        val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.2"
        val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
        val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
    }

    val okio = "com.squareup.okio:okio-multiplatform:2.9.0"
    val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:0.1.0"
    val junit = "junit:junit:${Versions.junit}"
    val stately = "co.touchlab:stately-common:${Versions.stately}"
    val multiplatformSettings = "com.russhwolf:multiplatform-settings:${Versions.multiplatformSettings}"
    val multiplatformSettingsTest = "com.russhwolf:multiplatform-settings-test:${Versions.multiplatformSettings}"
    val koinCore = "org.koin:koin-core:${Versions.koin}"
    val koinTest = "org.koin:koin-test:${Versions.koin}"
    val cocoapodsext = "co.touchlab:kotlinnativecocoapods:${Versions.cocoapodsext}"
    val kermit = "co.touchlab:kermit:${Versions.kermit}"
    val lifecycle_viewmodel = "android.arch.lifecycle:viewmodel:${Versions.lifecycle}"
    val lifecycle_viewmodel_extensions = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    val lifecycle_livedata = "android.arch.lifecycle:livedata:${Versions.lifecycle}"
    val lifecycle_extension = "android.arch.lifecycle:extensions:${Versions.lifecycle}"
    val karmok = "co.touchlab:karmok-library:${Versions.karmok}"
    val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"

    object AndroidXTest {
        val core = "androidx.test:core:${Versions.androidx_test}"
        val junit = "androidx.test.ext:junit:${Versions.androidx_test_ext}"
        val runner = "androidx.test:runner:${Versions.androidx_test}"
        val rules = "androidx.test:rules:${Versions.androidx_test}"
    }

    object KotlinTest {
        val common = "org.jetbrains.kotlin:kotlin-test-common:${Versions.kotlin}"
        val annotations = "org.jetbrains.kotlin:kotlin-test-annotations-common:${Versions.kotlin}"
        val jvm = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
        val junit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    }

    object Coroutines {
        val common = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    }

    object SqlDelight{
        val gradle = "com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}"
        val runtime = "com.squareup.sqldelight:runtime:${Versions.sqlDelight}"
        val runtimeJdk = "com.squareup.sqldelight:runtime-jvm:${Versions.sqlDelight}"
        val driverIos = "com.squareup.sqldelight:native-driver:${Versions.sqlDelight}"
        val driverAndroid = "com.squareup.sqldelight:android-driver:${Versions.sqlDelight}"
    }

    object Ktor {
        val commonCore = "io.ktor:ktor-client-core:${Versions.ktor}"
        val commonJson = "io.ktor:ktor-client-json:${Versions.ktor}"
        val commonLogging = "io.ktor:ktor-client-logging:${Versions.ktor}"
        val jvmCore = "io.ktor:ktor-client-core-jvm:${Versions.ktor}"
        val androidCore = "io.ktor:ktor-client-okhttp:${Versions.ktor}"
        val androidClient = "io.ktor:ktor-client-android:${Versions.ktor}"
        val jvmJson = "io.ktor:ktor-client-json-jvm:${Versions.ktor}"
        val jvmLogging = "io.ktor:ktor-client-logging-jvm:${Versions.ktor}"
        val ios = "io.ktor:ktor-client-ios:${Versions.ktor}"
        val iosCore = "io.ktor:ktor-client-core-native:${Versions.ktor}"
        val iosJson = "io.ktor:ktor-client-json-native:${Versions.ktor}"
        val iosLogging = "io.ktor:ktor-client-logging-native:${Versions.ktor}"
        val commonSerialization ="io.ktor:ktor-client-serialization:${Versions.ktor}"
        //val androidSerialization ="io.ktor:ktor-client-serialization-jvm:${Versions.ktor}"
    }
}
